/*
知识库使用方法：
包含7个文件 Dashboard.js,Dashboard.scss,DocumentsDashboard.js,DocumentsDetails.js,DocumentsList.js,DocumentsModel.js,DocumentsTreePanel.js
引用主页面xtype：dashboardView
//*/
Ext.define('Tab.view.dashboard.Dashboard', {
    extend: 'Ext.container.Container',
    xtype: 'dashboardView',

    requires: [
        'Ext.tab.Panel',
        'Tab.view.dashboard.DocumentsTreePanel',
        'Tab.view.dashboard.DocumentsList'
    ],

    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    padding: 10,
    items: [{
            xtype: 'container',

            itemId: 'navigationPanel',

            layout: {
                type: 'vbox',
                align: 'stretch'
            },

            width: '30%',
            minWidth: 180,
            maxWidth: 300,

            defaults: {
                cls: 'navigation-email',
                margin: '0 10 10 0'
            },
            items: [{
                xtype: 'documentstreepanel',
                listeners: {
                    itemclick: function (me) {
                        var panel = me.up();
                        var list = panel.up().up().down('documentslist');
                        var tabs = panel.up().up().down('tabpanel');
                        tabs.setActiveTab(list);
                        if (typeof (panel.store.proxy.extraParams.name) == 'undefined' || panel.store.proxy.extraParams.name.length == 0) {
                            list.getView().headerCt.child("#documentname").setText('目录');
                        } else {
                            list.getView().headerCt.child("#documentname").setText(panel.store.proxy.extraParams.name==='LBL_NONE' ? '无类别' : panel.store.proxy.extraParams.name);
                        }
                        list.documentstreepanel = panel;
                        list.store.proxy.url = '/tab/doc/ListDocuments';
                        Ext.apply(list.store.proxy.extraParams, {
                            id: panel.store.proxy.extraParams.id //tree,id
                        });
                        list.store.reload();
                    }
                }
            }]
        },
        {
            xtype: 'tabpanel',
            itemId: 'contentPanel',
            flex: 1,
            items: [{
                    xtype: 'documentsdashboard'
                },
                {
                    xtype: 'documentslist'
                }
            ]
        }
    ]
});