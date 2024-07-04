/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.mixins.transformers.feature.ice.common.logging;

import gg.essential.network.connectionmanager.ice.Log4jAsJulLogger;
import org.ice4j.StackProperties;
import org.ice4j.attribute.MessageIntegrityAttribute;
import org.ice4j.ice.Agent;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.NetworkUtils;
import org.ice4j.ice.harvest.AbstractTcpListener;
import org.ice4j.ice.harvest.AbstractUdpListener;
import org.ice4j.ice.harvest.AwsCandidateHarvester;
import org.ice4j.ice.harvest.CandidateHarvesterSet;
import org.ice4j.ice.harvest.GoogleTurnCandidateHarvest;
import org.ice4j.ice.harvest.HostCandidateHarvester;
import org.ice4j.ice.harvest.MappingCandidateHarvesters;
import org.ice4j.ice.harvest.SinglePortUdpHarvester;
import org.ice4j.ice.harvest.StunCandidateHarvest;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.StunMappingCandidateHarvester;
import org.ice4j.ice.harvest.TcpHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvest;
import org.ice4j.ice.harvest.UPNPHarvester;
import org.ice4j.message.Message;
import org.ice4j.message.MessageFactory;
import org.ice4j.pseudotcp.PseudoTCPBase;
import org.ice4j.socket.GoogleRelayedCandidateDatagramSocket;
import org.ice4j.socket.GoogleRelayedCandidateDelegate;
import org.ice4j.socket.GoogleRelayedCandidateSocket;
import org.ice4j.socket.IceTcpServerSocketWrapper;
import org.ice4j.socket.MultiplexedSocket;
import org.ice4j.socket.MultiplexingSocket;
import org.ice4j.socket.RelayedCandidateDatagramSocket;
import org.ice4j.stack.StunClientTransaction;
import org.ice4j.stack.StunStack;
import org.ice4j.stunclient.BlockingRequestSender;
import org.ice4j.stunclient.NetworkConfigurationDiscoveryProcess;
import org.ice4j.stunclient.SimpleAddressDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.logging.Logger;

/**
 * Jitsi has their own logging library (see IceManager) but does not actually use it consistently.
 * So, to use Log4j, for some classes we need to redirect their logger via Mixin.
 * Some of these classes reference `javax.sdp`, which we do not bundle, meaning they crash the game when force-loaded by a Mixin audit.
 * As such, they are commented out for the time being.
 */
@Mixin(value = {
    HostCandidateHarvester.class,
    StackProperties.class,
    MessageIntegrityAttribute.class,
    Agent.class,
    LocalCandidate.class,
    NetworkUtils.class,
    AbstractTcpListener.class,
    AbstractUdpListener.class,
    AwsCandidateHarvester.class,
    CandidateHarvesterSet.class,
    GoogleTurnCandidateHarvest.class,
    HostCandidateHarvester.class,
    MappingCandidateHarvesters.class,
    SinglePortUdpHarvester.class,
    StunCandidateHarvest.class,
    StunCandidateHarvester.class,
    StunMappingCandidateHarvester.class,
    TcpHarvester.class,
    TurnCandidateHarvest.class,
    UPNPHarvester.class,
//    IceSdpUtils.class, (uses SDP)
    Message.class,
    MessageFactory.class,
    PseudoTCPBase.class,
    GoogleRelayedCandidateDatagramSocket.class,
    GoogleRelayedCandidateDelegate.class,
    GoogleRelayedCandidateSocket.class,
    IceTcpServerSocketWrapper.class,
    MultiplexedSocket.class,
    MultiplexingSocket.class,
    RelayedCandidateDatagramSocket.class,
    StunClientTransaction.class,
    StunStack.class,
    BlockingRequestSender.class,
    NetworkConfigurationDiscoveryProcess.class,
    SimpleAddressDetector.class,
}, targets = {
    "org.ice4j.ice.harvest.CandidateHarvesterSetElement",
    "org.ice4j.ice.harvest.CandidateHarvesterSetTask",
    "org.ice4j.pseudotcp.PseudoTcpSocketImpl",
    "org.ice4j.socket.MultiplexingXXXSocketSupport",
    "org.ice4j.stack.Connector",
    "org.ice4j.stack.MessageProcessingTask",
    "org.ice4j.stack.NetAccessManager",
}, remap = false)
public class Mixin_RedirectIce4jJulLoggers {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/logging/Logger;getLogger(Ljava/lang/String;)Ljava/util/logging/Logger;"))
    private static Logger logAdapter(String name) {
        return new Log4jAsJulLogger(name);
    }
}
