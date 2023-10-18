Ext.define('Tab.store.NavigationTree', {
    extend: 'Ext.data.TreeStore',

    storeId: 'NavigationTree',

    fields: [{
        name: 'text'
    }],

    root: {
        expanded: true,
        children: [
            {
                text: 'Recording query',
                iconCls: 'x-fa fa-file-audio-o',
                resourceId: "45BBBF87-25B8-492A-B206-CC55F3E4903C", //录音查询
                viewType: 'searchrecordview',
                leaf: true
            },
            {
                text: 'voicemail query',
                iconCls: 'x-fa fa-file-audio-o',
                resourceId: "6C8605F6-B994-11E9-9EA9-54E1AD6C1F94", //语音信箱
                viewType: 'voicemailview',
                leaf: true
            },
            {
                text: 'Holiday',
                iconCls: 'x-fa fa-users',
                routeId: 'holidayView',
                viewType: 'holidayView',
                resourceId: "952C8D60-4F8A-11EB-B5EB-00155D782604", //假日配置
                leaf: true
            },
            {
            	text: 'Monitor',
                iconCls: 'x-fa fa-outdent',
                viewType: 'monitorView',
                routeId: 'monitorView',
                resourceId:'4A16A347-A2CF-11E9-9604-54E1AD6C1F93',//分机监控
                leaf: true
            },
            {
                text: 'Detial Reports ',
                iconCls: 'x-fa fa-line-chart',
                selectable: false,
                expanded: false,
                children: [
                    // {
                    //     text: ' monitorView ',
                    //     iconCls: 'x-fa fa-list',
                    //     viewType: 'monitorView',
                    //     routeId: 'monitorView',
                    //     resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F93',//综合报表
                    //     leaf: true
                    // }, 
                {
                    text: ' Cdr Voice ',
                    iconCls: 'x-fa fa-list',
                    viewType: 'cdr_voice',
                    routeId: 'cdr_voice',
                    resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F93',//综合报表
                    leaf: true
                }, 
                {
                    text: ' Miss Calls ',
                    iconCls: 'x-fa fa-list',
                    viewType: 'miss_calls',
                    routeId: 'miss_calls',
                    resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F94',//综合报表
                    leaf: true
                }, 
                {
                    text: ' Agent Login ',
                    iconCls: 'x-fa fa-list',
                    viewType: 'agent_login',
                    routeId: 'agent_login',
                    resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F93',
                    leaf: true
                }, 
                {
                    text: ' Agent State ',
                    iconCls: 'x-fa fa-list',
                    viewType: 'agent_state',
                    routeId: 'agent_state',
                    resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F93',
                    leaf: true
                }, 
            ]
            },

            {
                text: 'Summary Reports ',
                iconCls: 'x-fa fa-line-chart',
                selectable: false,
                expanded: false,
                children: [
                {
                    text: ' Agent Summary ',
                    iconCls: 'x-fa fa-list',
                    viewType: 'agentSummary',
                    routeId: 'agentSummary',
                    resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F93',
                    leaf: true
                }, 
                {
                    text: ' Queue Summary ',
                    iconCls: 'x-fa fa-list',
                    viewType: 'queueSummary',
                    routeId: 'queueSummary',
                    resourceId: 'BBFED234-B994-11E9-9EA9-54E1AD6C1F93',
                    leaf: true
                }, 
            ]
            },
        ]
    }
});