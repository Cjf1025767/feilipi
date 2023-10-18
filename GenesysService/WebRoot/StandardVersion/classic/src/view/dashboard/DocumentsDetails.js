Ext.define('Tab.view.dashboard.DocumentsDetails', {
    extend: 'Ext.form.Panel',
    xtype: 'documentsdetails',
    alias: 'widget.documentsdetails',
    requires: [
        'Ext.container.Container',
        'Tab.model.groupsStore'
    ],
    cls: 'shadow',
    bodyPadding: 10,
    layout: 'fit',
    listeners: {
        afterrender: function (box) {
            var me = box;
            if (me.documentid!==null) {
                Ext.Ajax.request({
                    url: "/tab/doc/GetDocument",
                    method: 'POST',
                    params: {
                        id: me.documentid
                    },
                    success: function (response, options) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            me.down('#contentId').setValue(obj.doc.content);
                            //me.down('#introductionId').setValue(obj.doc.introduction);
                            me.down('#titleId').setValue(obj.doc.subject); 
                            me.down('#category').setValue(obj.categoryname==='LBL_NONE' ? '无类别' : obj.categoryname);
                        }
                    },
                    failure: function (response, options) {

                    }
                });
            }else{
                me.down('#category').setValue(me.documentcategory==='LBL_NONE' ? '无类别' : me.documentcategory);
            }
        }
    },
    documentfatherid: null,
    documentsdashboard: null,
    documentstreepanel: null,
    documentslist: null,
    documentcategory: null,
    tbar: {
        cls: 'single-mail-action-button',
        defaults: {
            margin: '0 15 0 0'
        },
        layout: 'hbox',
        items: [{
                xtype: 'textfield',
                fieldLabel: '标题',
                itemId: 'titleId',
                labelWidth: 60,
                flex: 1,
                enableKeyEvents: true,
                listeners: {
                    keyup: function( me, e, eOpts ){
                        me.up('documentsdetails').down('#savebtnId').setDisabled(false);
                    }
                }
            },
            {
                xtype: 'textfield',
                width: 240,
                fieldLabel: '分类',
                itemId: 'category',
                readOnly: true,
                labelWidth: 60
            },
            {
                ui: 'soft-green',
                itemId: 'savebtnId',
                disabled: true,
                text: '保存',
                width: 80,
                handler: function (btn) {
                    var me = btn;
                    var main = me.up().up();
                    if (main.documentid !== null) {
                        Ext.Ajax.request({
                            url: "/tab/doc/UpdateDocument",
                            method: 'POST',
                            params: {
                                id: main.documentid,
                                content: main.down('#contentId').getValue(),
                                subject: main.down('#titleId').getValue(),
                                //introduction: main.down('#introductionId').getValue()
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    if (main.documentsdashboard != null) {
                                        main.documentsdashboard.down('#titleId').setHtml("<b>" + main.down('#titleId').getValue() + "</b>");
                                        main.documentsdashboard.setHtml(main.down('#contentId').getValue());
                                    }
                                    if (main.documentstreepanel != null) {
                                        var record = main.documentstreepanel.store.findNode('id', main.documentid);
                                        if (typeof (record) != 'undefined') record.set('text', main.down('#titleId').getValue());
                                    }
                                    if (main.documentslist != null) {
                                        main.documentslist.set('name', main.down('#titleId').getValue());
                                        main.documentslist.commit();
                                    }
                                    me.up('window').close();
                                } else {
                                    Ext.Msg.alert("错误", obj.msg);
                                }
                            },
                            failure: function (response, options) {
                                Ext.Msg.alert('错误', 'server-side failure with status code ' + response.status)
                            }
                        });
                    } else if (main.documentfatherid !== null) {
                        Ext.Ajax.request({
                            url: "/tab/doc/AddNewDocument",
                            method: 'POST',
                            params: {
                                category: main.documentfatherid,
                                content: main.down('#contentId').getValue(),
                                subject: main.down('#titleId').getValue()
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    if (main.documentstreepanel != null) {
                                        main.documentstreepanel.store.reload();
                                    }
                                    if (main.documentslist != null) {
                                        main.documentslist.store.reload();
                                    }
                                    me.up('window').close();
                                } else {
                                    Ext.Msg.alert("错误", obj.msg);
                                }
                            },
                            failure: function (response, options) {
                                Ext.Msg.alert('错误', 'server-side failure with status code ' + response.status)
                            }
                        });
                    }else{
                        Ext.Msg.alert('错误','需要选择一个分类');
                    }
                }
            }
        ]
    },
    items: [{
        xtype: 'htmleditor',
        itemId: 'contentId',
        fieldLabel: '内容',
        labelAlign: 'top',
        labelSeparator: '',
        enableFont: false,
        listeners: {
            change: function( me, newValue, oldValue, eOpts ){
               if(oldValue!==''){
                me.up('documentsdetails').down('#savebtnId').setDisabled(false);
               }
            }
        }
    }]
});