Ext.define('Rbac.view.tab.PasswordWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.passwordwindow',
    width: 320,
    height: 225,
    resizable: false,
    bodyPadding: '10 0 0 10',
    doDestroy: function () {
        this.callParent();
    },
    items: [{
            xtype: "textfield",
            itemId: "oldpassword",
            fieldLabel: "原密码",
            allowBlank: false,
            inputType: 'password'
        },
        {
            xtype: "textfield",
            itemId: "newpassword",
            fieldLabel: "新密码",
            allowBlank: false,
            inputType: 'password'
        },
        {
            xtype: "textfield",
            itemId: "renewpassword",
            fieldLabel: "重复新密码",
            allowBlank: false,
            inputType: 'password'
        }
    ],
    buttonAlign: 'right',
    buttons: [{
        xtype: "button",
        text: "确定",
        margin: '0 31 0 0',
        handler: function (btn) {
            var me = btn,
                newpassword = me.up('window').down('#newpassword').getValue(),
                oldpassword = me.up('window').down('#oldpassword').getValue(),
                renewpassword = me.up('window').down('#renewpassword').getValue();
            if (newpassword == renewpassword &&
                oldpassword.length > 0 &&
                newpassword.length > 0) {
                Ext.Ajax.request({
                    url: '/tab/rbac/UIChangePassword',
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    async: true,
                    params: {
                        password: globalVars.base64 ? window.btoa(oldpassword) : md5(oldpassword),
                        newpassword: globalVars.base64 ? window.btoa(newpassword) : md5(newpassword)
                    },
                    failure: function (response, opts) {
                        console.log('server-side failure with status code ' + response.status);
                    },
                    success: function (response, opts) {
                        var obj = Ext.decode(response.responseText);
                        if (obj.success) {
                            me.up("window").close();
                        } else {
                            Ext.Msg.alert('错误', obj.msg);
                            me.up('window').down('#newpassword').setValue('');
                            me.up('window').down('#oldpassword').setValue('');
                            me.up('window').down('#renewpassword').setValue('');
                        }

                    }

                });
            } else {
                Ext.Msg.alert('错误', '填写不完整或者两次新密码输入不一致');
                me.up('window').down('#newpassword').setValue('');
                me.up('window').down('#oldpassword').setValue('');
                me.up('window').down('#renewpassword').setValue('');
            }
        }
    }]
});