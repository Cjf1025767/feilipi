Ext.define('Tab.view.sms.smsTemplateController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.sms-smstemplate',

    onAddOrModifySmsTemplate: function (view, rowIndex, cellIndex, item, e, record, row) {
        var grid = view.up('gridpanel');
        var templatecode = ((record != null) ? record.get('templatecode') : null);
        Ext.create('Ext.window.Window', {
            title: (typeof record == 'undefined' || record == null) ? "新模板" : "修改模板",
            width: 420,
            height: 320,
            modal: true,
            resizable: false,
            listeners: {
                'afterrender': function (win) {
                    var me = win;
                    Ext.Ajax.request({
                        url: '/tab/call/PreviewSmsTemplate',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            templatecode: templatecode
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                var values = [];
                                obj.types.forEach(function (val) {
                                    values.push({
                                        name: val
                                    });
                                });
                                me.down('combobox').setStore(values);
                                if(obj.item!=null){
                                    me.down('combobox').setRawValue(obj.item.type);
                                    me.down('textfield[name=name]').setValue(obj.item.name);
                                    me.down('textfield[name=remark]').setValue(obj.item.remark);
                                    me.down('textfield[name=value]').setValue(obj.item.templatecontent);
                                }
                            }
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                }
            },
            items: [{
                xtype: 'form',
                items: [{
                    xtype: "fieldcontainer",
                    width: '100%',
                    height: 300,
                    padding: 5,
                    layout: "vbox",
                    defaults: {
                        labelWidth: 65
                    },
                    items: [{
                            xtype: "textfield",
                            width: '100%',
                            name: "name",
                            fieldLabel: "名称",
                            allowBlank: false
                        },
                        {
                            xtype: 'combobox',
                            fieldLabel: '短信类型',
                            autoLoad: true,
                            editable: false,
                            displayField: 'name',
                            valueField: 'id',
                            queryMmode: 'local',
                            allowBlank: false
                        },
                        {
                            xtype: "textfield",
                            width: '100%',
                            name: "remark",
                            fieldLabel: "申请描述",
                            allowBlank: false
                        },
                        {
                            xtype: "textareafield",
                            allowBlank: false,
                            width: '100%',
                            height: 60,
                            name: "value",
                            fieldLabel: "模板内容"
                        }
                    ]
                }]
            }],
            buttonAlign: 'center',
            buttons: [{
                xtype: "button",
                text: "确定",
                handler: function (btn) {
                    var me = btn;
                    Ext.Ajax.request({
                        url: '/tab/call/AddOrUpdateSmsTemplate',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            templatecode: templatecode,
                            name: me.up('window').down('textfield[name=name]').getValue(),
                            type: me.up('window').down('combobox').getRawValue(),
                            remark: me.up('window').down('textfield[name=remark]').getValue(),
                            value: me.up('window').down('textfield[name=value]').getValue()
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                grid.store.insert(0, obj.item);
                            }
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                    me.up('window').close();
                }
            }, {
                xtype: "button",
                text: "取消",
                handler: function (btn) {
                    btn.up('window').close();
                }
            }]
        }).show();
    }
});