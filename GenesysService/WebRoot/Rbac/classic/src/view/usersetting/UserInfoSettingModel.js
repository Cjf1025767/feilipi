Ext.define('Rbac.view.usersetting.UserInfoSettingModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.userinfosetting',
    data: {
        name: 'WebServer'
    }

});

Ext.define('Rbac.view.usersetting.UserSettingModel', {
    extend: 'Ext.data.Model',
    fields: [{
            name: 'nickname'
        },
        {
            name: 'userguid'
        },
        {
            name: 'headimgurl'
        }
    ]
});