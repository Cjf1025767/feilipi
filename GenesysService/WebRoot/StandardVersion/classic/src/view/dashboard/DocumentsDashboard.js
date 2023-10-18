Ext.define('Tab.view.dashboard.DocumentsDashboard', {
    extend: 'Ext.panel.Panel',
    xtype: 'documentsdashboard',
    requires: [
        'Tab.view.tab.iconlabel',
        'Tab.view.dashboard.DocumentsDetails',
        'Tab.view.tab.ResizeWindow'
    ],
    title: 'Bulletin board',
    scrollable: true,
    tbar: [{
            itemId: 'titleId',
            xtype: 'iconlabel',
            text: 'Title'
        },
        '->',
        {
            ui: 'soft-green',
            xtype: 'button',
            itemId: 'editbtn',
            text: 'Refresh',
            handler: function (btn) {
                var me = btn.up().up();
                Ext.Ajax.request({
                    url: "/tab/doc/GetDocument",
                    method: 'POST',
                    params: {
                        id: '00000000-0000-0000-0000-000000000000'
                    },
                    success: function (response, options) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            me.setHtml(obj.ann.description);
                            me.down('#titleId').setHtml("<b>" + obj.ann.subject + "</b>");
                        }
                    },
                    failure: function (response, options) {

                    }
                });
            }
        },
    ],
    listeners: {
        afterrender: function (box) {
            var me = box;
            Ext.Ajax.request({
                url: "/tab/doc/GetDocument",
                method: 'POST',
                params: {
                    id: '00000000-0000-0000-0000-000000000000'
                },
                success: function (response, options) {
                    var obj = Ext.decode(response.responseText);
                    if (obj.success) {
                        me.setHtml(obj.ann.description);
                        me.down('#titleId').setHtml("<b>" + obj.ann.subject + "</b> - " + Ext.Date.format(new Date(obj.ann.modifiedtime), 'Y-m-d H:i:s'));
                    }
                },
                failure: function (response, options) {

                }
            });
        }
    }
});