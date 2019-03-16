package com.redhat.cajun.navy.process.wih;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.redhat.cajun.navy.process.message.model.CreateMissionCommand;
import com.redhat.cajun.navy.process.message.model.Message;
import com.redhat.cajun.navy.rules.model.Mission;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.SettableListenableFuture;


public class KafkaMessageSenderWorkItemHandlerTest {

    @Mock
    private KafkaTemplate kafkaTemplate;

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkItemManager workItemManager;

    @Captor
    private ArgumentCaptor<Message<?>> messageCaptor;

    private KafkaMessageSenderWorkItemHandler wih;

    @Before
    public void setup() {
        initMocks(this);
        wih = new KafkaMessageSenderWorkItemHandler();
        setField(wih, null, kafkaTemplate, KafkaTemplate.class);
        setField(wih, "createMissionCommandDestination", "topic-mission-command", String.class);
        wih.init();
    }

    @Test
    public void testExecuteWorkItem() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("MessageType", "testMessageType");
        when(workItem.getParameters()).thenReturn(parameters);
        when(workItem.getId()).thenReturn(1L);

        wih.addPayloadBuilder("testMessageType", "topic-test", TestMessageEvent::build);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class))).thenReturn(new SettableListenableFuture());

        wih.executeWorkItem(workItem, workItemManager);
        verify(workItemManager).completeWorkItem(eq(1L), anyMap());
        verify(kafkaTemplate).send(eq("topic-test"), eq("testKey"), any(Message.class));
    }

    @Test
    public void testCreateMissionCommandMessageType() throws Exception {

        Mission mission = new Mission();
        mission.setIncidentId("incidentId");
        mission.setIncidentLat(new BigDecimal("30.12345"));
        mission.setIncidentLong(new BigDecimal("-70.98765"));
        mission.setResponderId("responderId");
        mission.setResponderStartLat(new BigDecimal("40.12345"));
        mission.setResponderStartLong(new BigDecimal("-80.98765"));
        mission.setDestinationLat(new BigDecimal("50.12345"));
        mission.setDestinationLong(new BigDecimal("-90.98765"));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("MessageType", "CreateMissionCommand");
        parameters.put("payload", mission);
        when(workItem.getParameters()).thenReturn(parameters);
        when(workItem.getId()).thenReturn(1L);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class))).thenReturn(new SettableListenableFuture());

        wih.executeWorkItem(workItem, workItemManager);
        verify(workItemManager).completeWorkItem(eq(1L), anyMap());
        verify(kafkaTemplate).send(eq("topic-mission-command"), eq("incidentId"), messageCaptor.capture());

        Message<CreateMissionCommand> message = (Message<CreateMissionCommand>) messageCaptor.getValue();
        assertThat(message.getMessageType(), equalTo("CreateMissionCommand"));
        assertThat(message.getInvokingService(), equalTo("ProcessService"));
        assertThat(message.getBody(), notNullValue());
        CreateMissionCommand cmd = message.getBody();
        assertThat(cmd.getIncidentId(), equalTo("incidentId"));
        assertThat(cmd.getIncidentLat(), equalTo("30.12345"));
        assertThat(cmd.getIncidentLong(), equalTo("-70.98765"));
        assertThat(cmd.getResponderId(), equalTo("responderId"));
        assertThat(cmd.getResponderStartLat(), equalTo("40.12345"));
        assertThat(cmd.getResponderStartLong(), equalTo("-80.98765"));
        assertThat(cmd.getDestinationLat(), equalTo("50.12345"));
        assertThat(cmd.getDestinationLong(), equalTo("-90.98765"));
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class TestMessageEvent {

        public static KafkaMessageSenderWorkItemHandler.Pair<String, TestMessageEvent> build(Map<String, Object> parameters) {
            TestMessageEvent event = new TestMessageEvent();
            return new KafkaMessageSenderWorkItemHandler.Pair<>("testKey", event);
        }

    }

}