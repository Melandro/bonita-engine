/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.core.process.definition.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.engine.bpm.connector.ConnectorDefinition;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.data.DataDefinition;
import org.bonitasoft.engine.bpm.document.DocumentDefinition;
import org.bonitasoft.engine.bpm.flownode.ActivityDefinition;
import org.bonitasoft.engine.bpm.flownode.CallActivityDefinition;
import org.bonitasoft.engine.bpm.flownode.EndEventDefinition;
import org.bonitasoft.engine.bpm.flownode.GatewayDefinition;
import org.bonitasoft.engine.bpm.flownode.IntermediateCatchEventDefinition;
import org.bonitasoft.engine.bpm.flownode.IntermediateThrowEventDefinition;
import org.bonitasoft.engine.bpm.flownode.StartEventDefinition;
import org.bonitasoft.engine.bpm.flownode.TransitionDefinition;
import org.bonitasoft.engine.bpm.flownode.impl.AutomaticTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.impl.HumanTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.ManualTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.ReceiveTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.SendTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.flownode.impl.UserTaskDefinitionImpl;
import org.bonitasoft.engine.bpm.process.SubProcessDefinition;
import org.bonitasoft.engine.bpm.userfilter.UserFilterDefinition;
import org.bonitasoft.engine.core.operation.model.builder.SOperationBuilders;
import org.bonitasoft.engine.core.process.definition.model.SActivityDefinition;
import org.bonitasoft.engine.core.process.definition.model.SBoundaryEventNotFoundException;
import org.bonitasoft.engine.core.process.definition.model.SConnectorDefinition;
import org.bonitasoft.engine.core.process.definition.model.SDocumentDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowElementContainerDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeDefinition;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeType;
import org.bonitasoft.engine.core.process.definition.model.SGatewayDefinition;
import org.bonitasoft.engine.core.process.definition.model.SGatewayType;
import org.bonitasoft.engine.core.process.definition.model.SNamedElement;
import org.bonitasoft.engine.core.process.definition.model.SSubProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.STransitionDefinition;
import org.bonitasoft.engine.core.process.definition.model.builder.ServerModelConvertor;
import org.bonitasoft.engine.core.process.definition.model.event.SBoundaryEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SEndEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SIntermediateCatchEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SIntermediateThrowEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SStartEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.impl.SEndEventDefinitionImpl;
import org.bonitasoft.engine.core.process.definition.model.event.impl.SIntermediateCatchEventDefinitionImpl;
import org.bonitasoft.engine.core.process.definition.model.event.impl.SIntermediateThrowEventDefinitionImpl;
import org.bonitasoft.engine.core.process.definition.model.event.impl.SStartEventDefinitionImpl;
import org.bonitasoft.engine.data.definition.model.SDataDefinition;
import org.bonitasoft.engine.data.definition.model.builder.SDataDefinitionBuilders;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilders;

/**
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class SFlowElementContainerDefinitionImpl extends SBaseElementImpl implements SFlowElementContainerDefinition {

    private static final long serialVersionUID = -40122166157566478L;

    private final Map<ConnectorEvent, List<SConnectorDefinition>> connectorsMap;

    private final Map<String, SFlowNodeDefinition> allElementsMapString;

    private final Map<String, SGatewayDefinition> gatewaysMap;

    private final Map<Long, SFlowNodeDefinition> allElementsMap;

    private final Map<String, SConnectorDefinition> allConnectorsMap;

    private final Map<String, STransitionDefinition> transitionsMap;

    private final Set<SActivityDefinition> activities;

    private final Set<STransitionDefinition> transitions;

    private final Set<SGatewayDefinition> gateways;

    private final Set<SFlowNodeDefinition> allElements;

    private final List<SConnectorDefinition> connectors;

    private final List<SStartEventDefinition> sStartEvents;

    private final List<SIntermediateCatchEventDefinition> sIntermediateCatchEvents;

    private final List<SBoundaryEventDefinition> sBoundaryEvents;

    private final List<SEndEventDefinition> sEndEvents;

    private final List<SIntermediateThrowEventDefinition> sIntermediateThrowEvents;

    private final List<SDataDefinition> sDataDefinitions;

    private final List<SDocumentDefinition> sDocumentDefinitions;

    private SNamedElement elementContainer;

    private boolean containsInclusiveGateway = false;

    public SFlowElementContainerDefinitionImpl() {
        activities = new HashSet<SActivityDefinition>();
        transitions = new HashSet<STransitionDefinition>();
        transitionsMap = new HashMap<String, STransitionDefinition>();
        gateways = new HashSet<SGatewayDefinition>();
        gatewaysMap = new HashMap<String, SGatewayDefinition>();
        allElements = new HashSet<SFlowNodeDefinition>();
        allElementsMap = new HashMap<Long, SFlowNodeDefinition>();
        allElementsMapString = new HashMap<String, SFlowNodeDefinition>();
        connectors = new ArrayList<SConnectorDefinition>();
        connectorsMap = new HashMap<ConnectorEvent, List<SConnectorDefinition>>(2);
        connectorsMap.put(ConnectorEvent.ON_ENTER, new ArrayList<SConnectorDefinition>());
        connectorsMap.put(ConnectorEvent.ON_FINISH, new ArrayList<SConnectorDefinition>());
        allConnectorsMap = new HashMap<String, SConnectorDefinition>(2);
        sStartEvents = new ArrayList<SStartEventDefinition>();
        sIntermediateCatchEvents = new ArrayList<SIntermediateCatchEventDefinition>();
        sIntermediateThrowEvents = new ArrayList<SIntermediateThrowEventDefinition>();
        sEndEvents = new ArrayList<SEndEventDefinition>();
        sDataDefinitions = new ArrayList<SDataDefinition>();
        sDocumentDefinitions = new ArrayList<SDocumentDefinition>();
        sBoundaryEvents = new ArrayList<SBoundaryEventDefinition>();
    }

    public SFlowElementContainerDefinitionImpl(final SNamedElement elementContainer, final FlowElementContainerDefinition container,
            final SExpressionBuilders sExpressionBuilders, final SDataDefinitionBuilders sDataDefinitionBuilders, final SOperationBuilders sOperationBuilders) {
        this.elementContainer = elementContainer;
        transitions = new HashSet<STransitionDefinition>();
        transitionsMap = new HashMap<String, STransitionDefinition>();
        for (final TransitionDefinition transition : container.getTransitions()) {
            final STransitionDefinitionImpl sTransitionDefinitionImpl = new STransitionDefinitionImpl(transition, sExpressionBuilders);
            addTransition(sTransitionDefinitionImpl);
        }

        allElements = new HashSet<SFlowNodeDefinition>();
        allElementsMap = new HashMap<Long, SFlowNodeDefinition>();
        allElementsMapString = new HashMap<String, SFlowNodeDefinition>();
        final List<ActivityDefinition> activities2 = container.getActivities();
        sBoundaryEvents = new ArrayList<SBoundaryEventDefinition>();
        activities = new HashSet<SActivityDefinition>(activities2.size());
        initializeActivities(activities2, sExpressionBuilders, sOperationBuilders, sDataDefinitionBuilders);

        final Set<GatewayDefinition> gateways2 = container.getGateways();
        gateways = new HashSet<SGatewayDefinition>(gateways2.size());
        gatewaysMap = new HashMap<String, SGatewayDefinition>(gateways2.size());
        final Iterator<GatewayDefinition> iterator1 = gateways2.iterator();
        while (iterator1.hasNext()) {
            final GatewayDefinition gatewayDefinition = iterator1.next();
            final SGatewayDefinitionImpl gateway;
            gateway = new SGatewayDefinitionImpl(gatewayDefinition, sExpressionBuilders, transitionsMap, sOperationBuilders);
            addGateway(gateway);
        }

        final List<ConnectorDefinition> connectors2 = container.getConnectors();
        final ArrayList<SConnectorDefinition> mConnectors = new ArrayList<SConnectorDefinition>(connectors2.size());
        connectorsMap = new HashMap<ConnectorEvent, List<SConnectorDefinition>>(2);
        connectorsMap.put(ConnectorEvent.ON_ENTER, new ArrayList<SConnectorDefinition>());
        connectorsMap.put(ConnectorEvent.ON_FINISH, new ArrayList<SConnectorDefinition>());
        for (final ConnectorDefinition connector : connectors2) {
            final SConnectorDefinitionImpl e = new SConnectorDefinitionImpl(connector, sExpressionBuilders, sOperationBuilders);
            mConnectors.add(e);
            connectorsMap.get(e.getActivationEvent()).add(e);
        }
        allConnectorsMap = new HashMap<String, SConnectorDefinition>(2);
        connectors = Collections.unmodifiableList(mConnectors);

        sStartEvents = initializeStartEvents(sExpressionBuilders, container.getStartEvents(), transitionsMap, sDataDefinitionBuilders, sOperationBuilders);
        sIntermediateCatchEvents = initializeIntermediateCatchEvents(sExpressionBuilders, container.getIntermediateCatchEvents(), transitionsMap,
                sDataDefinitionBuilders, sOperationBuilders);
        sIntermediateThrowEvents = initializeIntermediateThrowEvents(container.getIntermediateThrowEvents(), sExpressionBuilders, transitionsMap,
                sDataDefinitionBuilders, sOperationBuilders);
        sEndEvents = initializeEndEvents(sExpressionBuilders, container.getEndEvents(), transitionsMap, sDataDefinitionBuilders, sOperationBuilders);

        final List<DataDefinition> processDataDefinitions = container.getDataDefinitions();
        final ArrayList<SDataDefinition> mDataDefinitions = new ArrayList<SDataDefinition>(processDataDefinitions.size());
        for (final DataDefinition dataDefinition : processDataDefinitions) {
            mDataDefinitions.add(ServerModelConvertor.convertDataDefinition(dataDefinition, sDataDefinitionBuilders, sExpressionBuilders));
        }
        sDataDefinitions = Collections.unmodifiableList(mDataDefinitions);
        final List<DocumentDefinition> documentDefinitions2 = container.getDocumentDefinitions();
        final ArrayList<SDocumentDefinition> mDocumentDefinitions = new ArrayList<SDocumentDefinition>(documentDefinitions2.size());
        for (final DocumentDefinition documentDefinition : documentDefinitions2) {
            mDocumentDefinitions.add(new SDocumentDefinitionImpl(documentDefinition));
        }
        sDocumentDefinitions = Collections.unmodifiableList(mDocumentDefinitions);

    }

    private void initializeActivities(final List<ActivityDefinition> activities2, final SExpressionBuilders sExpressionBuilders,
            final SOperationBuilders sOperationBuilders, final SDataDefinitionBuilders sDataDefinitionBuilders) {
        final Iterator<ActivityDefinition> iterator = activities2.iterator();
        while (iterator.hasNext()) {
            final ActivityDefinition activityDefinition = iterator.next();
            final SActivityDefinitionImpl activity;
            if (activityDefinition instanceof AutomaticTaskDefinitionImpl) {
                activity = new SAutomaticTaskDefinitionImpl(activityDefinition, sExpressionBuilders, transitionsMap, sDataDefinitionBuilders,
                        sOperationBuilders);
            } else if (activityDefinition instanceof HumanTaskDefinitionImpl) {
                if (activityDefinition instanceof UserTaskDefinitionImpl) {
                    activity = new SUserTaskDefinitionImpl((UserTaskDefinitionImpl) activityDefinition, sExpressionBuilders, transitionsMap,
                            sDataDefinitionBuilders, sOperationBuilders);
                } else {
                    activity = new SManualTaskDefinitionImpl((ManualTaskDefinitionImpl) activityDefinition, sExpressionBuilders, transitionsMap,
                            sDataDefinitionBuilders, sOperationBuilders);
                }
                final HumanTaskDefinitionImpl humanTaskDefinitionImpl = (HumanTaskDefinitionImpl) activityDefinition;
                final UserFilterDefinition userFilter = humanTaskDefinitionImpl.getUserFilter();
                final SHumanTaskDefinitionImpl sHumanTaskDefinitionImpl = (SHumanTaskDefinitionImpl) activity;
                if (userFilter != null) {
                    sHumanTaskDefinitionImpl.setUserFilter(new SUserFilterDefinitionImpl(userFilter, sExpressionBuilders));
                }
                sHumanTaskDefinitionImpl.setPriority(humanTaskDefinitionImpl.getPriority());
                sHumanTaskDefinitionImpl.setExpectedDuration(humanTaskDefinitionImpl.getExpectedDuration());
            } else if (activityDefinition instanceof ReceiveTaskDefinitionImpl) {
                activity = new SReceiveTaskDefinitionImpl((ReceiveTaskDefinitionImpl) activityDefinition, sExpressionBuilders, transitionsMap,
                        sDataDefinitionBuilders, sOperationBuilders);
            } else if (activityDefinition instanceof SendTaskDefinitionImpl) {
                activity = new SSendTaskDefinitionImpl((SendTaskDefinitionImpl) activityDefinition, sExpressionBuilders, transitionsMap,
                        sDataDefinitionBuilders, sOperationBuilders);
            } else if (activityDefinition instanceof CallActivityDefinition) {
                activity = new SCallActivityDefinitionImpl((CallActivityDefinition) activityDefinition, sExpressionBuilders, transitionsMap,
                        sDataDefinitionBuilders, sOperationBuilders);
            } else if (activityDefinition instanceof SubProcessDefinition) {
                activity = new SSubProcessDefinitionImpl((SubProcessDefinition) activityDefinition, sExpressionBuilders, sDataDefinitionBuilders,
                        sOperationBuilders);
            } else {
                throw new BonitaRuntimeException("Can't find the client type for " + activityDefinition.getClass().getName());
            }
            addActivity(activity);
        }
    }

    private List<SEndEventDefinition> initializeEndEvents(final SExpressionBuilders sExpressionBuilders, final List<EndEventDefinition> endEvents,
            final Map<String, STransitionDefinition> transitionsMap, final SDataDefinitionBuilders sDataDefinitionBuilders,
            final SOperationBuilders sOperationBuilders) {
        final List<SEndEventDefinition> sEndEvents = new ArrayList<SEndEventDefinition>(endEvents.size());
        for (final EndEventDefinition endEventDefinition : endEvents) {
            final SEndEventDefinitionImpl sEndEvent = new SEndEventDefinitionImpl(endEventDefinition, sExpressionBuilders, transitionsMap,
                    sDataDefinitionBuilders, sOperationBuilders);
            sEndEvents.add(sEndEvent);
            allElements.add(sEndEvent);
            allElementsMap.put(sEndEvent.getId(), sEndEvent);
            allElementsMapString.put(sEndEvent.getName(), sEndEvent);
        }
        return sEndEvents;
    }

    private List<SStartEventDefinition> initializeStartEvents(final SExpressionBuilders sExpressionBuilders, final List<StartEventDefinition> startEvents,
            final Map<String, STransitionDefinition> transitionsMap, final SDataDefinitionBuilders sDataDefinitionBuilders,
            final SOperationBuilders sOperationBuilders) {
        final ArrayList<SStartEventDefinition> sStartEvents = new ArrayList<SStartEventDefinition>(startEvents.size());
        for (final StartEventDefinition startEventDefinition : startEvents) {
            final SStartEventDefinitionImpl sStartEvent = new SStartEventDefinitionImpl(startEventDefinition, sExpressionBuilders, transitionsMap,
                    sDataDefinitionBuilders, sOperationBuilders);
            sStartEvents.add(sStartEvent);
            allElements.add(sStartEvent);
            allElementsMap.put(sStartEvent.getId(), sStartEvent);
            allElementsMapString.put(sStartEvent.getName(), sStartEvent);
        }
        return sStartEvents;
    }

    private List<SIntermediateCatchEventDefinition> initializeIntermediateCatchEvents(final SExpressionBuilders sExpressionBuilders,
            final List<IntermediateCatchEventDefinition> intermediateCatchEvents, final Map<String, STransitionDefinition> transitionsMap,
            final SDataDefinitionBuilders sDataDefinitionBuilders, final SOperationBuilders sOperationBuilders) {
        final List<SIntermediateCatchEventDefinition> sIntermediateCatchEvents = new ArrayList<SIntermediateCatchEventDefinition>(
                intermediateCatchEvents.size());
        for (final IntermediateCatchEventDefinition intermediateCatchEventDefinition : intermediateCatchEvents) {
            final SIntermediateCatchEventDefinitionImpl sIntermediateCatchEvent = new SIntermediateCatchEventDefinitionImpl(intermediateCatchEventDefinition,
                    sExpressionBuilders, transitionsMap, sDataDefinitionBuilders, sOperationBuilders);
            sIntermediateCatchEvents.add(sIntermediateCatchEvent);
            allElements.add(sIntermediateCatchEvent);
            allElementsMap.put(sIntermediateCatchEvent.getId(), sIntermediateCatchEvent);
            allElementsMapString.put(sIntermediateCatchEvent.getName(), sIntermediateCatchEvent);
        }
        return sIntermediateCatchEvents;
    }

    private List<SIntermediateThrowEventDefinition> initializeIntermediateThrowEvents(final List<IntermediateThrowEventDefinition> intermediateThrowEvents,
            final SExpressionBuilders sExpressionBuilders, final Map<String, STransitionDefinition> transitionsMap,
            final SDataDefinitionBuilders sDataDefinitionBuilders, final SOperationBuilders sOperationBuilders) {
        final List<SIntermediateThrowEventDefinition> sIntermediateThrowEvents = new ArrayList<SIntermediateThrowEventDefinition>(
                intermediateThrowEvents.size());
        for (final IntermediateThrowEventDefinition intermediateThrowEventDefinition : intermediateThrowEvents) {
            final SIntermediateThrowEventDefinitionImpl sIntermediateThrowEvent = new SIntermediateThrowEventDefinitionImpl(intermediateThrowEventDefinition,
                    sExpressionBuilders, transitionsMap, sDataDefinitionBuilders, sOperationBuilders);
            sIntermediateThrowEvents.add(sIntermediateThrowEvent);
            allElements.add(sIntermediateThrowEvent);
            allElementsMap.put(sIntermediateThrowEvent.getId(), sIntermediateThrowEvent);
            allElementsMapString.put(sIntermediateThrowEvent.getName(), sIntermediateThrowEvent);
        }
        return sIntermediateThrowEvents;
    }

    public void addTransition(final STransitionDefinition transition) {
        transitions.add(transition);
        transitionsMap.put(transition.getName(), transition);
    }

    public void addActivity(final SActivityDefinition activity) {
        sBoundaryEvents.addAll(activity.getBoundaryEventDefinitions());
        for (final SBoundaryEventDefinition boundary : activity.getBoundaryEventDefinitions()) {
            sBoundaryEvents.add(boundary);
            allElements.add(boundary);
            allElementsMap.put(boundary.getId(), boundary);
        }
        activities.add(activity);
        allElements.add(activity);
        allElementsMap.put(activity.getId(), activity);
        allElementsMapString.put(activity.getName(), activity);
    }

    public void addGateway(final SGatewayDefinition gateway) {
        gateways.add(gateway);
        if (gateway.getGatewayType() == SGatewayType.INCLUSIVE) {
            containsInclusiveGateway = true;
        }
        gatewaysMap.put(gateway.getName(), gateway);
        allElements.add(gateway);
        allElementsMap.put(gateway.getId(), gateway);
        allElementsMapString.put(gateway.getName(), gateway);
    }

    public void addConnector(final SConnectorDefinition sConnectorDefinition) {
        connectors.add(sConnectorDefinition);
        connectorsMap.get(sConnectorDefinition.getActivationEvent()).add(sConnectorDefinition);
        // allConnectorsMap.put(sConnectorDefinition.getId(), sConnectorDefinition); // FIXME: Uncomment when generate id
        allConnectorsMap.put(sConnectorDefinition.getName(), sConnectorDefinition);
    }

    public void addStartEvent(final SStartEventDefinition startEvent) {
        sStartEvents.add(startEvent);
        allElements.add(startEvent);
        allElementsMap.put(startEvent.getId(), startEvent);
        allElementsMapString.put(startEvent.getName(), startEvent);
    }

    public void addIntemediateCatchEvent(final SIntermediateCatchEventDefinition event) {
        sIntermediateCatchEvents.add(event);
        allElements.add(event);
        allElementsMap.put(event.getId(), event);
        allElementsMapString.put(event.getName(), event);
    }

    public void addEndEvent(final SEndEventDefinition endEvent) {
        sEndEvents.add(endEvent);
        allElements.add(endEvent);
        allElementsMap.put(endEvent.getId(), endEvent);
        allElementsMapString.put(endEvent.getName(), endEvent);
    }

    public void addIntermediateThrowEvent(final SIntermediateThrowEventDefinition intermediateThrowEvent) {
        sIntermediateThrowEvents.add(intermediateThrowEvent);
        allElements.add(intermediateThrowEvent);
        allElementsMap.put(intermediateThrowEvent.getId(), intermediateThrowEvent);
        allElementsMapString.put(intermediateThrowEvent.getName(), intermediateThrowEvent);
    }

    public void addDataDefinition(final SDataDefinition dataDefinition) {
        sDataDefinitions.add(dataDefinition);
    }

    @Override
    public Set<SGatewayDefinition> getGateways() {
        return gateways;
    }

    @Override
    public SGatewayDefinition getGateway(final String name) {
        SGatewayDefinition sGatewayDefinition = gatewaysMap.get(name);
        if (sGatewayDefinition == null) {
            sGatewayDefinition = getGatewayFromSubProcesses(name);
        }
        return sGatewayDefinition;
    }

    private SGatewayDefinition getGatewayFromSubProcesses(final String name) {
        boolean found = false;
        SGatewayDefinition gateway = null;
        final Iterator<SActivityDefinition> iterator = activities.iterator();
        while (iterator.hasNext() && !found) {
            final SActivityDefinition activityDefinition = iterator.next();
            if (SFlowNodeType.SUB_PROCESS.equals(activityDefinition.getType())) {
                gateway = ((SSubProcessDefinition) activityDefinition).getSubProcessContainer().getGateway(name);
                if (gateway != null) {
                    found = true;
                }
            }
        }
        return gateway;
    }

    @Override
    public Set<SFlowNodeDefinition> getFlowNodes() {
        return allElements;
    }

    @Override
    public SFlowNodeDefinition getFlowNode(final long id) {
        SFlowNodeDefinition flowNodeDefinition = allElementsMap.get(id);
        if (flowNodeDefinition == null) {
            flowNodeDefinition = getFlowNodeFromSubProcesses(id);
        }
        return flowNodeDefinition;
    }

    @Override
    public SFlowNodeDefinition getFlowNode(final String targetFlowNode) {
        SFlowNodeDefinition flowNodeDefinition = allElementsMapString.get(targetFlowNode);
        if (flowNodeDefinition == null) {
            flowNodeDefinition = getFlowNodeFromSubProcesses(targetFlowNode);
        }
        return flowNodeDefinition;
    }

    private SFlowNodeDefinition getFlowNodeFromSubProcesses(final long id) {
        boolean found = false;
        SFlowNodeDefinition flowNode = null;
        final Iterator<SActivityDefinition> iterator = activities.iterator();
        while (iterator.hasNext() && !found) {
            final SActivityDefinition activityDefinition = iterator.next();
            if (SFlowNodeType.SUB_PROCESS.equals(activityDefinition.getType())) {
                flowNode = ((SSubProcessDefinition) activityDefinition).getSubProcessContainer().getFlowNode(id);
                if (flowNode != null) {
                    found = true;
                }
            }
        }
        return flowNode;
    }

    private SFlowNodeDefinition getFlowNodeFromSubProcesses(final String targetFlowNode) {
        boolean found = false;
        SFlowNodeDefinition flowNode = null;
        final Iterator<SActivityDefinition> iterator = activities.iterator();
        while (iterator.hasNext() && !found) {
            final SActivityDefinition activityDefinition = iterator.next();
            if (SFlowNodeType.SUB_PROCESS.equals(activityDefinition.getType())) {
                flowNode = ((SSubProcessDefinition) activityDefinition).getSubProcessContainer().getFlowNode(targetFlowNode);
                if (flowNode != null) {
                    found = true;
                }
            }
        }
        return flowNode;
    }

    @Override
    public STransitionDefinition getTransition(final String name) {
        STransitionDefinition transitionDefinition = transitionsMap.get(name);
        if (transitionDefinition == null) {
            transitionDefinition = getTransitionFromSubProcesses(name);
        }
        return transitionDefinition;
    }

    private STransitionDefinition getTransitionFromSubProcesses(final String name) {
        boolean found = false;
        STransitionDefinition transition = null;
        final Iterator<SActivityDefinition> iterator = activities.iterator();
        while (iterator.hasNext() && !found) {
            final SActivityDefinition activityDefinition = iterator.next();
            if (SFlowNodeType.SUB_PROCESS.equals(activityDefinition.getType())) {
                transition = ((SSubProcessDefinition) activityDefinition).getSubProcessContainer().getTransition(name);
                if (transition != null) {
                    found = true;
                }
            }
        }
        return transition;
    }

    @Override
    public List<SConnectorDefinition> getConnectors() {
        return connectors;
    }

    @Override
    // public SConnectorDefinition getConnectorDefinition(final long id) {// FIXME: Uncomment when generate id
    public SConnectorDefinition getConnectorDefinition(final String name) {
        return allConnectorsMap.get(name);
    }

    @Override
    public List<SConnectorDefinition> getConnectors(final ConnectorEvent connectorEvent) {
        return connectorsMap.get(connectorEvent);
    }

    @Override
    public List<SStartEventDefinition> getStartEvents() {
        return sStartEvents;
    }

    @Override
    public List<SIntermediateCatchEventDefinition> getIntermediateCatchEvents() {
        return sIntermediateCatchEvents;
    }

    @Override
    public List<SEndEventDefinition> getEndEvents() {
        return sEndEvents;
    }

    @Override
    public List<SDataDefinition> getDataDefinitions() {
        return sDataDefinitions;
    }

    @Override
    public List<SIntermediateThrowEventDefinition> getIntermdiateThrowEvents() {
        return Collections.unmodifiableList(sIntermediateThrowEvents);
    }

    @Override
    public List<SDocumentDefinition> getDocumentDefinitions() {
        return sDocumentDefinitions;
    }

    public void addDocumentDefinition(final SDocumentDefinition documentDefinition) {
        sDocumentDefinitions.add(documentDefinition);
    }

    @Override
    public Set<SActivityDefinition> getActivities() {
        return activities;
    }

    @Override
    public Set<STransitionDefinition> getTransitions() {
        return transitions;
    }

    @Override
    public SNamedElement getElementContainer() {
        return elementContainer;
    }

    public void setElementContainer(final SNamedElement elementContainer) {
        this.elementContainer = elementContainer;
    }

    @Override
    public List<SBoundaryEventDefinition> getBoundaryEvents() {
        return Collections.unmodifiableList(sBoundaryEvents);
    }

    @Override
    public SBoundaryEventDefinition getBoundaryEvent(final String name) throws SBoundaryEventNotFoundException {
        boolean found = false;
        SBoundaryEventDefinition boundaryEvent = null;
        final Iterator<SBoundaryEventDefinition> iterator = sBoundaryEvents.iterator();
        while (iterator.hasNext() && !found) {
            final SBoundaryEventDefinition currentBoundaryEvent = iterator.next();
            if (currentBoundaryEvent.getName().equals(name)) {
                found = true;
                boundaryEvent = currentBoundaryEvent;
            }
        }
        return boundaryEvent;
    }

    @Override
    public boolean containsInclusiveGateway() {
        return containsInclusiveGateway;
    }

}
