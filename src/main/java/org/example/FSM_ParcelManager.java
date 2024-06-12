package org.example;

import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.Condition;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

public class FSM_ParcelManager {

    // 定义快递状态
    enum ParcelState {
        PENDING, COLLECTED, PAYING, PAID, TRANSITING, DELIVERY, DELIVERED, REFUSED, EXCEPTION, CANCELLED, COMPLETED
    }

    // 定义快递事件
    enum ParcelEvent {
        COLLECT, PAY, SHIP, CANCEL, AUTO, DELIVERY
    }

    // 定义角色
    enum Role {
        POSTMAN, SENDER, TRANSITER, DRIVER, RECIPIENT, AUTO
    }

    // 定义状态机类
    @StateMachineParameters(stateType=ParcelState.class, eventType=ParcelEvent.class, contextType=Role.class)
    static class ParcelStateMachine extends AbstractUntypedStateMachine {
        protected void onTransition(ParcelState from, ParcelState to, ParcelEvent event, Role role) {
            System.out.println("Transition from '" + from + "' to '" + to + "' on event '" + event +
                    "' with role '" + role + "'.");
        }

        protected void onEntry(ParcelState state) {
            System.out.println("Entering State '" + state + "'.");
        }
    }

    public static void main(String[] args) {
        // 构建状态转换
        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(ParcelStateMachine.class);

        // 小哥权限
        builder.externalTransition().from(ParcelState.PENDING).to(ParcelState.COLLECTED).on(ParcelEvent.COLLECT).when(roleEquals(Role.POSTMAN, "POSTMAN_COLLECT")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.TRANSITING).to(ParcelState.DELIVERY).on(ParcelEvent.DELIVERY).when(roleEquals(Role.POSTMAN, "POSTMAN_DELIVERY")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.DELIVERY).to(ParcelState.EXCEPTION).on(ParcelEvent.CANCEL).when(roleEquals(Role.POSTMAN, "POSTMAN_CANCEL")).callMethod("onTransition");

        // 寄件人权限
        builder.externalTransition().from(ParcelState.PAYING).to(ParcelState.PAID).on(ParcelEvent.PAY).when(roleEquals(Role.SENDER, "SENDER_PAY")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.PENDING).to(ParcelState.CANCELLED).on(ParcelEvent.CANCEL).when(roleEquals(Role.SENDER, "SENDER_CANCEL")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.PAYING).to(ParcelState.CANCELLED).on(ParcelEvent.CANCEL).when(roleEquals(Role.SENDER, "SENDER_CANCEL_PAYING")).callMethod("onTransition");

        // 转运员权限
        builder.externalTransition().from(ParcelState.PAID).to(ParcelState.TRANSITING).on(ParcelEvent.SHIP).when(roleEquals(Role.TRANSITER, "TRANSITER_SHIP")).callMethod("onTransition");

        // 司机权限
        builder.externalTransition().from(ParcelState.PAID).to(ParcelState.TRANSITING).on(ParcelEvent.SHIP).when(roleEquals(Role.DRIVER, "DRIVER_SHIP")).callMethod("onTransition");

        // 收件人权限
        builder.externalTransition().from(ParcelState.DELIVERY).to(ParcelState.DELIVERED).on(ParcelEvent.DELIVERY).when(roleEquals(Role.RECIPIENT, "RECIPIENT_DELIVERED")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.DELIVERY).to(ParcelState.REFUSED).on(ParcelEvent.CANCEL).when(roleEquals(Role.RECIPIENT, "RECIPIENT_REFUSED")).callMethod("onTransition");

        // AUTO权限
        builder.externalTransition().from(ParcelState.DELIVERED).to(ParcelState.COMPLETED).on(ParcelEvent.AUTO).when(roleEquals(Role.AUTO, "AUTO_COMPLETED")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.CANCELLED).to(ParcelState.COMPLETED).on(ParcelEvent.AUTO).when(roleEquals(Role.AUTO, "AUTO_CANCELLED_COMPLETED")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.REFUSED).to(ParcelState.COMPLETED).on(ParcelEvent.AUTO).when(roleEquals(Role.AUTO, "AUTO_REFUSED_COMPLETED")).callMethod("onTransition");
        builder.externalTransition().from(ParcelState.EXCEPTION).to(ParcelState.COMPLETED).on(ParcelEvent.AUTO).when(roleEquals(Role.AUTO, "AUTO_EXCEPTION_COMPLETED")).callMethod("onTransition");
        // AUTO自动转换已揽收到待支付的逻辑
        builder.externalTransition().from(ParcelState.COLLECTED).to(ParcelState.PAYING).on(ParcelEvent.AUTO).callMethod("onTransition");



        // 定义进入状态时的动作
        for (ParcelState state : ParcelState.values()) {
            builder.onEntry(state).callMethod("onEntry");
        }

        // 使用状态机
        UntypedStateMachine fsm = builder.newStateMachine(ParcelState.PENDING);
        fsm.fire(ParcelEvent.COLLECT, Role.POSTMAN); // 小哥揽收

        fsm.fire(ParcelEvent.AUTO, Role.AUTO); // 自动转换到待支付

        fsm.fire(ParcelEvent.PAY, Role.SENDER); // 寄件人支付

        fsm.fire(ParcelEvent.SHIP, Role.TRANSITER); // 转运员发货

        fsm.fire(ParcelEvent.DELIVERY, Role.DRIVER); // 司机派送

        fsm.fire(ParcelEvent.DELIVERY, Role.POSTMAN); // 小哥派送

        fsm.fire(ParcelEvent.DELIVERY, Role.RECIPIENT); // 收件人签收

        fsm.fire(ParcelEvent.AUTO, Role.AUTO); // 自动完成
    }

    // 判断角色是否符合的方法
    private static Condition<Object> roleEquals(final Role role, final String identifier) {
        return new Condition<Object>() {
            @Override
            public boolean isSatisfied(Object context) {
                return context == role;
            }

            @Override
            public String name() {
                return "roleEquals_" + identifier;
            }
        };
    }

}
