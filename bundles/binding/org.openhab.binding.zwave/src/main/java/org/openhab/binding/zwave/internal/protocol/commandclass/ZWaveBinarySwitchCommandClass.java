/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

import java.util.ArrayList;
import java.util.Collection;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageType;
import org.openhab.binding.zwave.internal.protocol.NodeStage;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Handles the Binary Switch command class. Binary switches can be turned
 * on or off and report their status as on (0xFF) or off (0x00).
 * The commands include the possibility to set a given level, get a given
 * level and report a level.
 * @author Jan-Willem Spuij
 * @since 1.3.0
 */
@XStreamAlias("binarySwitchCommandClass")
public class ZWaveBinarySwitchCommandClass extends ZWaveCommandClass implements ZWaveBasicCommands, ZWaveCommandClassDynamicState {

	private static final Logger logger = LoggerFactory.getLogger(ZWaveBinarySwitchCommandClass.class);
	
	private static final int SWITCH_BINARY_SET = 0x01;
	private static final int SWITCH_BINARY_GET = 0x02;
	private static final int SWITCH_BINARY_REPORT = 0x03;
	
	/**
	 * Creates a new instance of the ZWaveBinarySwitchCommandClass class.
	 * @param node the node this command class belongs to
	 * @param controller the controller to use
	 * @param endpoint the endpoint this Command class belongs to
	 */
	public ZWaveBinarySwitchCommandClass(ZWaveNode node,
			ZWaveController controller, ZWaveEndpoint endpoint) {
		super(node, controller, endpoint);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandClass getCommandClass() {
		return CommandClass.SWITCH_BINARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleApplicationCommandRequest(SerialMessage serialMessage,
			int offset, int endpoint) {
		logger.trace("Handle Message Switch Binary Request");
		logger.debug(String.format("Received Switch Binary Request for Node ID = %d", this.getNode().getNodeId()));
		int command = serialMessage.getMessagePayloadByte(offset);
		switch (command) {
			case SWITCH_BINARY_SET:
				logger.trace("Process Switch Binary Set");
				logger.debug("Switch Binary Set sent to the controller will be processed as Switch Binary Report");
				// Now, some devices report their value as a switch binary set. For instance the Aeon Labs Micro Smart Energy Switch.
				// Process this as if it was a value report.
				processSwitchBinaryReport(serialMessage, offset, endpoint);
				break;
			case SWITCH_BINARY_GET:
				logger.warn(String.format("Command 0x%02X not implemented.", command));
				return;
			case SWITCH_BINARY_REPORT:
				logger.trace("Process Switch Binary Report");
				processSwitchBinaryReport(serialMessage, offset, endpoint);
				
				if (this.getNode().getNodeStage() != NodeStage.DONE)
					this.getNode().advanceNodeStage(NodeStage.DONE);
				break;
			default:
			logger.warn(String.format("Unsupported Command 0x%02X for command class %s (0x%02X).", 
					command, 
					this.getCommandClass().getLabel(),
					this.getCommandClass().getKey()));
		}
	}

	/**
	 * Processes a SWITCH_BINARY_REPORT / SWITCH_BINARY_SET message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processSwitchBinaryReport(SerialMessage serialMessage, int offset,
			int endpoint) {
		int value = serialMessage.getMessagePayloadByte(offset + 1); 
		logger.debug(String.format("Switch Binary report from nodeId = %d, value = 0x%02X", this.getNode().getNodeId(), value));
		ZWaveCommandClassValueEvent zEvent = new ZWaveCommandClassValueEvent(this.getNode().getNodeId(), endpoint, this.getCommandClass(), value);
		this.getController().notifyEventListeners(zEvent);
	}
	
	/**
	 * Gets a SerialMessage with the SWITCH_BINARY_GET command 
	 * @return the serial message
	 */
	public SerialMessage getValueMessage() {
		logger.debug("Creating new message for application command SWITCH_BINARY_GET for node {}", this.getNode().getNodeId());
		SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler, SerialMessagePriority.Get);
    	byte[] newPayload = { 	(byte) this.getNode().getNodeId(), 
    							2, 
								(byte) getCommandClass().getKey(), 
								(byte) SWITCH_BINARY_GET };
    	result.setMessagePayload(newPayload);
    	return result;		
	}
	
	/**
	 * Gets a SerialMessage with the SWITCH_BINARY_SET command 
	 * @param the level to set. 0 is mapped to off, > 0 is mapped to on.
	 * @return the serial message
	 */
	public SerialMessage setValueMessage(int level) {
		logger.debug("Creating new message for application command SWITCH_BINARY_SET for node {}", this.getNode().getNodeId());
		SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.SendData, SerialMessagePriority.Set);
    	byte[] newPayload = { 	(byte) this.getNode().getNodeId(), 
    							3, 
								(byte) getCommandClass().getKey(), 
								(byte) SWITCH_BINARY_SET,
								(byte) (level > 0 ? 0xFF : 0x00)
								};
    	result.setMessagePayload(newPayload);
    	return result;		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<SerialMessage> getDynamicValues() {
		ArrayList<SerialMessage> result = new ArrayList<SerialMessage>();
		
		result.add(getValueMessage());
		
		return result;
	}
}
