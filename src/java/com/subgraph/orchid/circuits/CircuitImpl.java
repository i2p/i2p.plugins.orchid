package com.subgraph.orchid.circuits;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.subgraph.orchid.Cell;
import com.subgraph.orchid.Circuit;
import com.subgraph.orchid.CircuitNode;
import com.subgraph.orchid.Connection;
import com.subgraph.orchid.DirectoryCircuit;
import com.subgraph.orchid.ExitCircuit;
import com.subgraph.orchid.InternalCircuit;
import com.subgraph.orchid.RelayCell;
import com.subgraph.orchid.Router;
import com.subgraph.orchid.Stream;
import com.subgraph.orchid.StreamConnectFailedException;
import com.subgraph.orchid.TorException;
import com.subgraph.orchid.circuits.path.CircuitPathChooser;
import com.subgraph.orchid.circuits.path.PathSelectionFailedException;
import com.subgraph.orchid.dashboard.DashboardRenderable;
import com.subgraph.orchid.dashboard.DashboardRenderer;

/**
 * This class represents an established circuit through the Tor network.
 *
 */
public abstract class CircuitImpl implements Circuit, DashboardRenderable {
	protected final static Logger logger = Logger.getLogger(CircuitImpl.class.getName());
	
	static ExitCircuit createExitCircuit(CircuitManagerImpl circuitManager, Router exitRouter) {
		return new ExitCircuitImpl(circuitManager, exitRouter);
	}
	
	static ExitCircuit createExitCircuitTo(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
		return new ExitCircuitImpl(circuitManager, prechosenPath);
	}
	
	static DirectoryCircuit createDirectoryCircuit(CircuitManagerImpl circuitManager) {
		return new DirectoryCircuitImpl(circuitManager, null);
	}
	
	static DirectoryCircuit createDirectoryCircuitTo(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
		return new DirectoryCircuitImpl(circuitManager, prechosenPath);
	}
	
	static InternalCircuit createInternalCircuitTo(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
		return new InternalCircuitImpl(circuitManager, prechosenPath);
	}

	private final CircuitManagerImpl circuitManager;
	protected final List<Router> prechosenPath;
	
	private final List<CircuitNode> nodeList;
	private final CircuitStatus status;

	private CircuitIO io;


	
		
	
	
	protected CircuitImpl(CircuitManagerImpl circuitManager) {
		this(circuitManager, null);
	}
	
	protected CircuitImpl(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
		nodeList = new ArrayList<CircuitNode>();
		this.circuitManager = circuitManager;
		this.prechosenPath = prechosenPath;
		status = new CircuitStatus();
	}

	List<Router> choosePath(CircuitPathChooser pathChooser) throws InterruptedException, PathSelectionFailedException {
		if(prechosenPath != null) {
			return new ArrayList<Router>(prechosenPath);
		} else {
			return choosePathForCircuit(pathChooser);
		}
	}

	protected abstract List<Router> choosePathForCircuit(CircuitPathChooser pathChooser) throws InterruptedException, PathSelectionFailedException;

	void bindToConnection(Connection connection) {
		if(io != null) {
			throw new IllegalStateException("Circuit already bound to a connection");
		}
		final int id = connection.bindCircuit(this);
		io = new CircuitIO(this, connection, id);
	}

	public void markForClose() {
		if(io != null) {
			io.markForClose();
		}
	}

	public boolean isMarkedForClose() {
		if(io == null) {
			return false;
		} else {
			return io.isMarkedForClose();
		}
	}
	
	CircuitStatus getStatus() {
		return status;
	}
	
	public boolean isConnected() {
		return status.isConnected();
	}

	public boolean isPending() {
		return status.isBuilding();
	}
	
	public boolean isClean() {
		return !status.isDirty();
	}
	
	/** @since 1.2.2 */
	public boolean isClosed() {
		return status.isClosed();
	}
	
	public int getSecondsDirty() {
		return (int) (status.getMillisecondsDirty() / 1000);
	}

	void notifyCircuitBuildStart() {
		if(!status.isUnconnected()) {
			throw new IllegalStateException("Can only connect UNCONNECTED circuits");
		}
		status.updateCreatedTimestamp();
		status.setStateBuilding();
		circuitManager.addActiveCircuit(this);
	}
	
	void notifyCircuitBuildFailed() {
		status.setStateFailed();
		circuitManager.removeActiveCircuit(this);
	}
	
	void notifyCircuitBuildCompleted() {
		status.setStateOpen();
		status.updateCreatedTimestamp();
	}
	
	public Connection getConnection() {
		if(!isConnected())
			throw new TorException("Circuit is not connected.");
		return io.getConnection();
	}

	public int getCircuitId() {
		if(io == null) {
			return 0;
		} else {
			return io.getCircuitId();
		}
	}

	public void sendRelayCell(RelayCell cell) {
		io.sendRelayCellTo(cell, cell.getCircuitNode());
	}

	public void sendRelayCellToFinalNode(RelayCell cell) {
		io.sendRelayCellTo(cell, getFinalCircuitNode());
	}

	public void appendNode(CircuitNode node) {
		nodeList.add(node);
	}

	List<CircuitNode> getNodeList() {
		return nodeList;
	}

	int getCircuitLength() {
		return nodeList.size();
	}

	public CircuitNode getFinalCircuitNode() {
		if(nodeList.isEmpty())
			throw new TorException("getFinalCircuitNode() called on empty circuit");
		return nodeList.get( getCircuitLength() - 1);
	}

	public RelayCell createRelayCell(int relayCommand, int streamId, CircuitNode targetNode) {
		return io.createRelayCell(relayCommand, streamId, targetNode);
	}

	public RelayCell receiveRelayCell() {
		return io.dequeueRelayResponseCell();
	}

	void sendCell(Cell cell) {
		io.sendCell(cell);
	}
	
	Cell receiveControlCellResponse() {
		return io.receiveControlCellResponse();
	}

	/*
	 * This is called by the cell reading thread in ConnectionImpl to deliver control cells 
	 * associated with this circuit (CREATED or CREATED_FAST).
	 */
	public void deliverControlCell(Cell cell) {
		io.deliverControlCell(cell);
	}

	/* This is called by the cell reading thread in ConnectionImpl to deliver RELAY cells. */
	public void deliverRelayCell(Cell cell) {
		io.deliverRelayCell(cell);
	}

	protected StreamImpl createNewStream(boolean autoclose) {
		return io.createNewStream(autoclose);
	}
	protected StreamImpl createNewStream() {
		return createNewStream(false);
	}

	void setStateDestroyed() {
		status.setStateDestroyed();
		circuitManager.removeActiveCircuit(this);
	}

	public void destroyCircuit() {
		// We might not have bound this circuit yet
		if (io != null) {
			io.destroyCircuit();
		}
		circuitManager.removeActiveCircuit(this);
	}


	public void removeStream(StreamImpl stream) {
		io.removeStream(stream);
	}

	protected Stream processStreamOpenException(Exception e) throws InterruptedException, TimeoutException, StreamConnectFailedException {
		if(e instanceof InterruptedException) {
			throw (InterruptedException) e;
		} else if(e instanceof TimeoutException) {
			throw(TimeoutException) e;
		} else if(e instanceof StreamConnectFailedException) {
			throw(StreamConnectFailedException) e;
		} else {
			throw new IllegalStateException();
		}
	}
	
	protected abstract String getCircuitTypeLabel();
	
	public String toString() {
		return "<tr class=\"circuit\"><td title=\"Circuit ID\">" + getCircuitId() + "</td><td>" +
				getCircuitTypeLabel() + "</td><td>" +
				status.getStateAsString().replace("Open ", "").replace("[", "").replace("]", "") + "</td><td>" +
//				"bandwidth usage stats here</td><td> -->" +
				pathToString() + "</td></tr>";
	}

	protected String pathToString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<span class=\"circuitcontainer\">");
		for(CircuitNode node: nodeList) {
			Router r = node.getRouter();
			if(sb.length() > 1)
				sb.append(" <span class=\"hidden\">-> </span>");
			sb.append("<span class=\"nodecontainer\"><span class=\"hidden\">[</span><span class=\"node\" onclick=\"copyText();\"><span class=\"flag\" data-country=\"");
			if (r != null) {
				if (r.getCountryName() != null)
					sb.append(r.getCountryName() + " (" + r.getAddress() + ")");
				else
					sb.append(r.getAddress());
			} else {
				sb.append("unknown");
		}
			sb.append("\">");
			sb.append("<img height=\"11\" width=\"16\" src=\"/flags.jsp?c=");
			if (r != null && r.getCountryName() != null)
				sb.append(r.getCountryCode().toLowerCase().replace("--", "a0"));
			else
				sb.append("a0");
			sb.append("\"></span>");
			if (r != null) {
				String idHash = r.getIdentityHash().toString().toUpperCase();
				int uptime = r.getUptime();
				int bw = r.getObservedBandwidth();
				sb.append("<span class=\"nickname");
				if (r.getPlatform() != null || r.getUptime() > 0 || r.getObservedBandwidth() > 0)
					sb.append("\" data-ipv4=\"");

				if (r.getPlatform() != null)
					sb.append(r.getPlatform().replace("Tor ", "").replace(" on ", " / ").replace("-alpha-dev", "-alpha"));

				if (uptime > 0 && bw > 0)
					sb.append(" \u2022 ");
				if (bw > 0 && bw < 1048576)
					sb.append((bw / 1024) + " KB/s");
				else if (bw >= 1048576)
					sb.append(((bw / 1024) / 1024) + " MB/s");

				if (uptime > 0)
					sb.append(" \u2022 Up: ");
				if (uptime > 172800)
					sb.append((((uptime / 60) / 60) / 24) + " days");
				else if (uptime > 1440)
					sb.append(((uptime / 60) / 60) + " hours");
				else if (uptime > 3600)
					sb.append(uptime / 60 + " minutes");
				else if (uptime > 0)
					sb.append(uptime + " seconds");

				sb.append("\">");

				sb.append("<a class=\"script\" href=\"https://metrics.torproject.org/rs.html#search/" + idHash + "\" target=\"_blank\">" + node.toString() + "</a>");

				// <noscript> alternative lookup
				sb.append("<noscript>");
				sb.append("<a href=\"https://torstatus.blutmagie.de/router_detail.php?FP=" + idHash + "\" target=\"_blank\">" + node.toString() + "</a>");
				sb.append("</noscript>");

				sb.append("</span><span class=\"hidden\"> (<b>" + r.getAddress() + "</b>)</span></span><span class=\"hidden\">]</span></span>");
			} else {
				sb.append("<span class=\"nickname unknown\"><i>unknown</i></span>");
			sb.append("</span><span class=\"hidden\">]</span></span>");
			}
		}
		sb.append("</span>");
		return sb.toString();
	}

	public List<Stream> getActiveStreams() {
		if(io == null) {
			return Collections.emptyList();
		} else {
			return io.getActiveStreams();
		}
	}

	public void dashboardRender(DashboardRenderer renderer, PrintWriter writer, int flags) throws IOException {
		if(io != null) {
			writer.println(toString());
			renderer.renderComponent(writer, flags, io);
		}
	}	
}
