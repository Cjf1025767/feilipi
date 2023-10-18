Ext.define('Rbac.view.record.MarkWindow', {
	extend: 'Ext.window.Window',
	xtype: 'form-contact-window',
	title: '备注',
    minWidth: 300,
    minHeight: 380,
    layout: 'fit',
    resizable: true,
    modal: true,
    defaultFocus: 'Subject',
    closeAction: 'hide',
    width: 400,
    listeners: {
		'afterrender': function () {
			var me = this;
            if(typeof me.params.username != 'undefined'){
                me.down('textfield[name=username]').setValue(me.params.username);
            }
            if(typeof me.params.memo != 'undefined'){
                me.down('textfield[name=memo]').setValue(me.params.memo);
            }
        }
    },
    items: [{
        xtype: 'form',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        border: false,
        bodyPadding: 10,

        fieldDefaults: {
            msgTarget: 'side',
            labelAlign: 'top',
            labelWidth: 100,
            labelStyle: 'font-weight:bold'
        },

        items: [{
            xtype: 'textfield',
            name: 'username',
            fieldLabel: '客户名称',
            afterLabelTextTpl: [
                '<span style="color:red;font-weight:bold" data-qtip="Required">*</span>'
            ],
            allowBlank: false
        }, {
            xtype: 'textareafield',
            name: 'memo',
            fieldLabel: '备注',
            labelAlign: 'top',
            flex: 1,
            margin: '0',
            afterLabelTextTpl: [
                '<span style="color:red;font-weight:bold" data-qtip="Required">*</span>'
            ],
            allowBlank: false
        }],

        buttons: [{
            text: '取消',
            handler: function(){
                this.up().up().getForm().reset();
                this.up().up().up().close();
            }
        }, {
            text: '保存',
            handler: function(){
                var me = this;
                var formPanel = me.up().up(),
                form = formPanel.getForm();
    
                if (form.isValid()) {
                    var text = JSON.stringify({
                        username:formPanel.down('textfield[name=username]').getValue(),
                        memo:formPanel.down('textfield[name=memo]').getValue()
                    });
                    Ext.Ajax.request({
                        url: '/tab/rec/UIUpdateRecfileMark',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        async: true,
                        params:{
                            id: formPanel.up().recordId,
                            mark: text
                        },
                        success: function (response, options) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                formPanel.up().record.set('sMark',text);
                                form.reset();
                                formPanel.up().close();
                            }
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        }
                    });
                }
            }
        }]
    }]
});