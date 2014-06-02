package org.opennms.netmgt.model.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeTopology {

	private static final Logger LOG = LoggerFactory
			.getLogger(BridgeTopology.class);

	public enum BridgePortRole {
		BACKBONE, DIRECT
	};

	public class BridgeTopologyLinkCandidate {

		private final BridgeTopologyPort bridgeTopologyPort;
		private Set<String> macs = new HashSet<String>();
		private Set<Integer> targets = new HashSet<Integer>();
		private BridgePortRole role;
		private BridgeTopologyPort linkportcandidate;

		public BridgeTopologyLinkCandidate(BridgeTopologyPort btp) {
			bridgeTopologyPort = btp;
		}

		public void removeMacs(Set<String> otherMacs) {
			Set<String> curmacs = new HashSet<String>();
			for (String mac : getMacs()) {
				if (otherMacs.contains(mac))
					continue;
				curmacs.add(mac);
			}
			role = BridgePortRole.BACKBONE;
			macs = curmacs;
		}

		public Set<String> getMacs() {
			if (macs.isEmpty())
				return bridgeTopologyPort.getMacs();
			return macs;
		}

		public boolean intersectionNull(
				BridgeTopologyLinkCandidate portcandidate) {
			for (String mac : getMacs()) {
				if (portcandidate.getMacs().contains(mac))
					return false;
			}
			return true;
		}

		public void merge(BridgeTopologyLinkCandidate other) {
			for (String mac : other.macs) {
				if (bridgeTopologyPort.getMacs().contains(mac))
					macs.add(mac);
			}
		}

		public boolean strictContained(BridgeTopologyLinkCandidate portcandidate) {
			if (portcandidate.getMacs().size() <= getMacs().size())
				return false;
			for (String mac : getMacs()) {
				if (!portcandidate.getMacs().contains(mac))
					return false;
			}
			return true;
		}

		public BridgeTopologyPort getBridgeTopologyPort() {
			return bridgeTopologyPort;
		}

		public Set<Integer> getTargets() {
			return targets;
		}

		public void addTarget(Integer target) {
			this.targets.add(target);
		}

		public BridgePortRole getRole() {
			return role;
		}

		public void setRole(BridgePortRole role) {
			this.role = role;
		}

		public BridgeTopologyPort getLinkPortCandidate() {
			return linkportcandidate;
		}

		public void setLinkPortCandidate(BridgeTopologyPort linkportcandidate) {
			this.linkportcandidate = linkportcandidate;
		}

	}

	public class BridgeTopologyPort {
		private final Integer nodeid;
		private final Integer bridgePort;
		private final Set<String> macs;

		public BridgeTopologyPort(Integer nodeid, Integer bridgePort,
				Set<String> macs) {
			super();
			this.nodeid = nodeid;
			this.bridgePort = bridgePort;
			this.macs = macs;
		}

		public Set<String> getMacs() {
			return macs;
		}

		public Integer getNodeid() {
			return nodeid;
		}

		public Integer getBridgePort() {
			return bridgePort;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((bridgePort == null) ? 0 : bridgePort.hashCode());
			result = prime * result
					+ ((nodeid == null) ? 0 : nodeid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BridgeTopologyPort other = (BridgeTopologyPort) obj;
			if (bridgePort == null) {
				if (other.bridgePort != null)
					return false;
			} else if (!bridgePort.equals(other.bridgePort))
				return false;
			if (nodeid == null) {
				if (other.nodeid != null)
					return false;
			} else if (!nodeid.equals(other.nodeid))
				return false;
			return true;
		}
	}

	public class BridgeTopologyLink {
		final private BridgeTopologyPort bridgePort;
		private BridgeTopologyPort designatebridgePort;

		private Set<String> macs = new HashSet<String>();

		public BridgeTopologyLink(BridgeTopologyPort bridgeport) {
			super();
			this.bridgePort = bridgeport;
			macs = bridgeport.getMacs();
		}

		public BridgeTopologyLink(BridgeTopologyPort bridgeport,
				BridgeTopologyPort designatedbridgePort) {
			super();
			this.bridgePort = bridgeport;
			this.designatebridgePort = designatedbridgePort;
			for (String mac : bridgeport.getMacs()) {
				if (designatedbridgePort.getMacs().contains(mac))
					macs.add(mac);
			}
		}

		public Set<String> getMacs() {
			return macs;
		}

		public BridgeTopologyPort getBridgeTopologyPort() {
			return bridgePort;
		}

		public BridgeTopologyPort getDesignateBridgePort() {
			return designatebridgePort;
		}

		public boolean contains(BridgeTopologyPort bridgeport) {
			if (this.bridgePort.equals(bridgeport))
				return true;
			if (this.designatebridgePort != null
					&& this.designatebridgePort.equals(bridgeport))
				return true;
			return false;
		}

	}

	private List<BridgeTopologyLink> bridgelinks = new ArrayList<BridgeTopologyLink>();
	private Map<String, Set<BridgeTopologyPort>> bridgeAssociatedMacAddressMap = new HashMap<String, Set<BridgeTopologyPort>>();
	private List<BridgeTopologyLinkCandidate> bridgeTopologyPortCandidates = new ArrayList<BridgeTopologyLinkCandidate>();

	public void addBridgeAssociatedMac(Integer nodeid, Integer port,
			Set<String> macsonport, String mac) {
		LOG.info(
				"addBridgeAssociatedMac: adding nodeid {}, bridge port {}, mac {}",
				nodeid, port, mac);
		if (bridgeAssociatedMacAddressMap.containsKey(mac))
			bridgeAssociatedMacAddressMap.get(mac).add(
					new BridgeTopologyPort(nodeid, port, macsonport));
		else {
			Set<BridgeTopologyPort> ports = new HashSet<BridgeTopologyPort>();
			ports.add(new BridgeTopologyPort(nodeid, port, macsonport));
			bridgeAssociatedMacAddressMap.put(mac, ports);
		}
	}

	private boolean parsed(BridgeTopologyPort bridgePort) {
		for (BridgeTopologyLink link : bridgelinks) {
			if (link.contains(bridgePort))
				return true;
		}
		return false;
	}


	public void parseBFT(Integer nodeid, Map<Integer,Set<String>> bridgeForwardingTable) {
		LOG.info(
				"addNodeToTopology: parsing node {},",
				nodeid);

		// parsing bridge forwarding table
		for (final Entry<Integer, Set<String>> curEntry : bridgeForwardingTable.entrySet()) {
			BridgeTopologyPort bridgetopologyport = new BridgeTopologyPort(
					nodeid, curEntry.getKey(), curEntry.getValue());

			if (parsed(bridgetopologyport)) {
				LOG.info(
						"addNodeToTopology: node {}, port {} has been previuosly parsed. Skipping.",
						nodeid, curEntry.getKey());
				continue;
			}

			BridgeTopologyLinkCandidate topologycandidate = new BridgeTopologyLinkCandidate(
					bridgetopologyport);
			for (String mac : curEntry.getValue()) {
				if (bridgeAssociatedMacAddressMap.containsKey(mac)) {
					for (BridgeTopologyPort swPort : bridgeAssociatedMacAddressMap
							.get(mac)) {
						if (swPort.getNodeid().intValue() == nodeid)
							continue;
						LOG.info(
								"addNodeToTopology: node {}, port {}: mac {} found on bridge adding target: targetnodeid {}, targetport {}",
								nodeid, curEntry.getKey(), mac,
								swPort.getNodeid(), swPort.getBridgePort());
						topologycandidate.setLinkPortCandidate(swPort);
						topologycandidate.addTarget(swPort.getNodeid());
					}
				}
			}
			bridgeTopologyPortCandidates.add(parseBFTEntry(topologycandidate));
		}
		mergeTopology();
	}

	public BridgeTopologyLinkCandidate parseBFTEntry(
			BridgeTopologyLinkCandidate topologyLinkCandidate) {
		/*
		 * This class is designed to get the topology on one bridge forwarding
		 * table at a time so this means that the rules are written considering
		 * port1 belonging always to the same bridge.
		 * 
		 * 
		 * We assume the following:
		 * 
		 * 1) there where no loops into the network (so there is a hierarchy)
		 * 
		 * Corollary 1
		 * 
		 * If exists there is only one backbone port from sw1 and sw2 If exists
		 * there is only one backbone port from sw2 and sw1
		 * 
		 * Corollary 2 There is only one "pseudo device" containing the bridge
		 * 
		 * Corollary 3 on a backbone port two different mac address must belong
		 * to the same pseudo device
		 */
		for (BridgeTopologyLinkCandidate linkcandidate : bridgeTopologyPortCandidates) {
			// regola same node non faccio niente
			if (linkcandidate.getBridgeTopologyPort().getNodeid().intValue() == topologyLinkCandidate
					.getBridgeTopologyPort().getNodeid().intValue()) {
				LOG.info(
						"parseBFTEntry: rule 0: same node candidate nodeid {}, port{}:  do nothing checking nodeid{}, port{}, macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());
				continue;
			}
			// regola intersezione nulla non faccio niente
			if (linkcandidate.intersectionNull(topologyLinkCandidate)) {
				LOG.info(
						"parseBFTEntry: rule 00: mac intesection null candidate nodeid {}, port{}: do nothing checking node {}, port {}: macs {}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());
				continue;
			}

			if (linkcandidate.getRole() == BridgePortRole.BACKBONE) {
				LOG.info(
						"parseBFTEntry: rule 1-d: setting candidate node {} port{} to DIRECT: checking node {}, BACKBONE port{}, macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());
				linkcandidate.removeMacs(topologyLinkCandidate.getMacs());
				topologyLinkCandidate.setRole(BridgePortRole.DIRECT);
				continue;
			}

			if (topologyLinkCandidate.getRole() == BridgePortRole.BACKBONE) {
				LOG.info(
						"parseBFTEntry: rule 1-r: candidate node{} BACKBONE port{}: setting node {} port{} macs{} to DIRECT",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());
				topologyLinkCandidate.removeMacs(linkcandidate.getMacs());
				linkcandidate.setRole(BridgePortRole.DIRECT);
				continue;
			}

			// regola della dipendenza assoluta new
			if (topologyLinkCandidate.strictContained(linkcandidate)) {
				LOG.info(
						"parseBFTEntry: rule 2-d: candidate node{} DIRECT port{} strict contained: checking node {} BACKBONE port {} macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());

				linkcandidate.setRole(BridgePortRole.BACKBONE);
				linkcandidate.removeMacs(topologyLinkCandidate.getMacs());
				linkcandidate.addTarget(topologyLinkCandidate
						.getBridgeTopologyPort().getNodeid());
				topologyLinkCandidate.setRole(BridgePortRole.DIRECT);
				continue;
			}

			// regola della dipendenza assoluta old
			if (linkcandidate.strictContained(topologyLinkCandidate)) {
				LOG.info(
						"parseBFTEntry: rule 2-r: candidate node{} BACKBONE port{} strict contains: checking node {} DIRECT port {} macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());
				topologyLinkCandidate.setRole(BridgePortRole.BACKBONE);
				topologyLinkCandidate.removeMacs(linkcandidate.getMacs());
				topologyLinkCandidate.addTarget(linkcandidate
						.getBridgeTopologyPort().getNodeid());
				linkcandidate.setRole(BridgePortRole.DIRECT);
				continue;
			}

			if (linkcandidate.getLinkPortCandidate() == null
					&& topologyLinkCandidate.getLinkPortCandidate() == null) {
				LOG.info(
						"parseBFTEntry: rule 3: set candidate each other candidate: candidate node{} port{}: checking node {} port {} macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());
				linkcandidate.setLinkPortCandidate(topologyLinkCandidate
						.getBridgeTopologyPort());
				topologyLinkCandidate.setLinkPortCandidate(linkcandidate
						.getBridgeTopologyPort());
				continue;
			}

			if (linkcandidate.getLinkPortCandidate() != null
					&& topologyLinkCandidate.getBridgeTopologyPort()
							.getNodeid().intValue() == linkcandidate
							.getLinkPortCandidate().getNodeid().intValue()
					&& topologyLinkCandidate.getBridgeTopologyPort()
							.getBridgePort().intValue() != linkcandidate
							.getLinkPortCandidate().getBridgePort().intValue()) {
				LOG.info(
						"parseBFTEntry: rule 4-d: candidate node{} DIRECT port{} strict contained: checking node {} BACKBONE port {} macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());

				linkcandidate.setRole(BridgePortRole.BACKBONE);
				linkcandidate.removeMacs(topologyLinkCandidate.getMacs());
				linkcandidate.addTarget(topologyLinkCandidate
						.getBridgeTopologyPort().getNodeid());

				topologyLinkCandidate.setRole(BridgePortRole.DIRECT);
				topologyLinkCandidate.setLinkPortCandidate(null);
				continue;
			}

			if (topologyLinkCandidate.getLinkPortCandidate() != null
					&& linkcandidate.getBridgeTopologyPort().getNodeid()
							.intValue() == topologyLinkCandidate
							.getLinkPortCandidate().getNodeid().intValue()
					&& linkcandidate.getBridgeTopologyPort().getBridgePort()
							.intValue() != topologyLinkCandidate
							.getLinkPortCandidate().getBridgePort().intValue()) {
				LOG.info(
						"parseBFTEntry: rule 4-r: candidate node{} BACKBONE port{} strict contains: checking node {} DIRECT port {} macs{}",
						topologyLinkCandidate.getBridgeTopologyPort()
								.getNodeid(), topologyLinkCandidate
								.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getBridgeTopologyPort().getNodeid(),
						linkcandidate.getBridgeTopologyPort().getBridgePort(),
						linkcandidate.getMacs());

				topologyLinkCandidate.setRole(BridgePortRole.BACKBONE);
				topologyLinkCandidate.removeMacs(linkcandidate.getMacs());
				topologyLinkCandidate.addTarget(linkcandidate
						.getBridgeTopologyPort().getNodeid());

				linkcandidate.setRole(BridgePortRole.DIRECT);
				linkcandidate.setLinkPortCandidate(null);
				continue;
			}
		}
		return topologyLinkCandidate;
	}

	public void parseSTPEntry(Integer nodeid, Integer bridgePort,
			Set<String> macs, Integer designatednodeid, Integer designatedport,
			Set<String> designatedmacs) {

		BridgeTopologyPort source = new BridgeTopologyPort(nodeid, bridgePort,
				macs);
		BridgeTopologyPort designated = new BridgeTopologyPort(
				designatednodeid, designatedport, designatedmacs);
		BridgeTopologyLinkCandidate sourceLink = new BridgeTopologyLinkCandidate(
				source);
		BridgeTopologyLinkCandidate designatedLink = new BridgeTopologyLinkCandidate(
				designated);
		BridgeTopologyLink link = new BridgeTopologyLink(source, designated);
		LOG.info("parseSTPEntry: macs on bridge {}.", macs);
		LOG.info("parseSTPEntry: macs on designated bridge {}.", designatedmacs);
		LOG.info(
				"parseSTPEntry: nodeid {}, port {}, designated nodeid {}, designated port {}, macs on link {}.",
				nodeid, bridgePort, designatednodeid, designatedport,
				link.getMacs());
		if (sourceLink.intersectionNull(designatedLink)) {
			bridgelinks.add(link);
		} else {
			sourceLink.addTarget(designatednodeid);
			designatedLink.addTarget(nodeid);
			bridgeTopologyPortCandidates.add(parseBFTEntry(sourceLink));
			bridgeTopologyPortCandidates.add(parseBFTEntry(designatedLink));
		}
	}

	private void mergeTopology() {
		// first: cannot have two backbone from one bridge, so if a backbone and
		// b with candidate, then b is direct
		Set<BridgeTopologyLinkCandidate> secondStep = new HashSet<BridgeTopology.BridgeTopologyLinkCandidate>();
		for (BridgeTopologyLinkCandidate candidateA : bridgeTopologyPortCandidates) {
			if (candidateA.getRole() != BridgePortRole.BACKBONE)
				continue;
			for (BridgeTopologyLinkCandidate candidateB : bridgeTopologyPortCandidates) {
				if (candidateB.getBridgeTopologyPort().getNodeid().intValue() != candidateA
						.getBridgeTopologyPort().getNodeid().intValue())
					continue;
				if (candidateB.getRole() != null)
					continue;
				if (candidateB.getLinkPortCandidate() == null)
					continue;
				if (candidateA.getTargets().contains(
						candidateB.getLinkPortCandidate().getNodeid())) {
					LOG.info(
							"mergeTopology: rule A: node {} BACKBONE port {}: only one backbone port is allowed for targets {}: setting port {} to DIRECT",
							candidateA.getBridgeTopologyPort().getNodeid(),
							candidateA.getBridgeTopologyPort().getBridgePort(),
							candidateA.getTargets(), candidateB
									.getBridgeTopologyPort().getBridgePort());
					candidateB.setRole(BridgePortRole.DIRECT);
					candidateB.setLinkPortCandidate(null);
					secondStep.add(candidateB);
				}
			}
		}
		// second: if a contains mac and is direct and b contains mac: then b is
		// backbone
		for (BridgeTopologyLinkCandidate candidateA : secondStep) {
			if (candidateA.getRole() != BridgePortRole.DIRECT)
				continue;
			for (BridgeTopologyLinkCandidate candidateB : bridgeTopologyPortCandidates) {
				if (candidateB.getBridgeTopologyPort().getNodeid().intValue() == candidateA
						.getBridgeTopologyPort().getNodeid().intValue())
					continue;
				if (candidateB.getRole() == BridgePortRole.DIRECT)
					continue;
				Set<String> otherMacs = new HashSet<String>();
				for (String mac : candidateA.getMacs()) {
					if (candidateB.getMacs().contains(mac)) {
						otherMacs.add(mac);
					}
				}
				if (otherMacs.isEmpty())
					continue;

				LOG.info(
						"mergeTopology: rule B: macs {}: node {} DIRECT port {}: removing from node {} BACKBONE port {} ",
						otherMacs, candidateA.getBridgeTopologyPort()
								.getNodeid(), candidateA
								.getBridgeTopologyPort().getBridgePort(),
						candidateB.getBridgeTopologyPort().getNodeid(),
						candidateB.getBridgeTopologyPort().getBridgePort());
				candidateB.removeMacs(otherMacs);
				candidateB.addTarget(candidateA.getBridgeTopologyPort()
						.getNodeid());
			}
		}

		// reset all roles
		for (BridgeTopologyLinkCandidate candidate : bridgeTopologyPortCandidates) {
			candidate.setRole(null);
		}
	}

	// FIXME
	public List<BridgeTopologyLink> getTopology() {
		Set<BridgeTopologyPort> foundedOnBridgeLink = new HashSet<BridgeTopologyPort>();

		for (BridgeTopologyLinkCandidate candidateA : bridgeTopologyPortCandidates) {
			if (foundedOnBridgeLink
					.contains(candidateA.getBridgeTopologyPort()))
				continue;
			if (candidateA.getTargets().isEmpty()) {
				continue;
			}
			LOG.info(
					"getTopology: bridgetobridge discovery: parsing nodeidA {}, portA {}, targetsA {}.",
					candidateA.getBridgeTopologyPort().getNodeid(), candidateA
							.getBridgeTopologyPort().getBridgePort(),
					candidateA.getTargets());
			for (BridgeTopologyLinkCandidate candidateB : bridgeTopologyPortCandidates) {
				if (foundedOnBridgeLink.contains(candidateB
						.getBridgeTopologyPort()))
					continue;
				if (candidateB.getTargets().isEmpty())
					continue;
				if (candidateA.getBridgeTopologyPort().getNodeid().intValue() == candidateB
						.getBridgeTopologyPort().getNodeid().intValue())
					continue;
				LOG.info(
						"getTopology: bridgetobridge discovery: parsing nodeidB {}, portB {}, targetsB {}.",
						candidateB.getBridgeTopologyPort().getNodeid(),
						candidateB.getBridgeTopologyPort().getBridgePort(),
						candidateB.getTargets());
				if (candidateA.getTargets().contains(
						candidateB.getBridgeTopologyPort().getNodeid())
						&& candidateB.getTargets().contains(
								candidateA.getBridgeTopologyPort().getNodeid())) {
					foundedOnBridgeLink.add(candidateB.getBridgeTopologyPort());
					foundedOnBridgeLink.add(candidateA.getBridgeTopologyPort());
					BridgeTopologyLink link = new BridgeTopologyLink(
							candidateA.getBridgeTopologyPort(),
							candidateB.getBridgeTopologyPort());
					LOG.info(
							"getTopology: bridgetobridge discovery: link found {}",
							link);
					bridgelinks.add(link);
				}
			}
		}
		for (BridgeTopologyLinkCandidate candidate : bridgeTopologyPortCandidates) {
			if (foundedOnBridgeLink.contains(candidate.getBridgeTopologyPort()))
				continue;
			LOG.info(
					"getTopology: mac discovery: parsing nodeid {}, port {}, macs {}, targets {}.",
					candidate.getBridgeTopologyPort().getNodeid(), candidate
							.getBridgeTopologyPort().getBridgePort(), candidate
							.getMacs(), candidate.getTargets());
			if (candidate.getLinkPortCandidate() == null
					|| candidate.getRole() == BridgePortRole.DIRECT) {
				BridgeTopologyLink link = new BridgeTopologyLink(
						new BridgeTopologyPort(candidate
								.getBridgeTopologyPort().getNodeid(), candidate
								.getBridgeTopologyPort().getBridgePort(),
								candidate.getMacs()));
				LOG.info("getTopology: bridgetomac link found {}", link);
				bridgelinks.add(link);
				continue;
			}
			if (foundedOnBridgeLink.contains(candidate.getLinkPortCandidate())) {
				continue;
			}
			BridgeTopologyLink link = new BridgeTopologyLink(
					new BridgeTopologyPort(candidate.getBridgeTopologyPort()
							.getNodeid(), candidate.getBridgeTopologyPort()
							.getBridgePort(), candidate.getMacs()),
					candidate.getLinkPortCandidate());
			LOG.info(
					"getTopology: bridgebridge link found using port associated mac {}",
					link);
			bridgelinks.add(link);
		}
		return bridgelinks;
	}

}