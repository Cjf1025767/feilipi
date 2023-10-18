Ext.define('Rbac.Application', {
    extend: 'Ext.app.Application',
    requires: ['Rbac.view.tab.ActivityMonitor'],
    name: 'Rbac',

    stores: [
        'NavigationTree'
    ],

    defaultToken: 'dashboard',

    // The name of the initial view to create. This class will gain a "viewport" plugin
    // if it does not extend Ext.Viewport.
    //
    mainView: 'Rbac.view.main.Main',
    launch: function () {
        //使用ExtJS的cookie保持当前状态
        Ext.state.Manager.setProvider(Ext.create('Ext.state.CookieProvider', {
            expires: new Date(new Date().getTime() + (1000 * 60 * 60 * 24 * 30 * 3))
        })); //cookie超时时间：90 days
        Ext.Ajax.request({
            url: '/tab/rbac/UIIsLogin',
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            failure: function (response, opts) {
                console.log('server-side failure with status code ' + response.status);
                window.location.href = "/login.html";
            },
            success: function (response, options) {
                var obj = Ext.decode(response.responseText);
                if (obj != null && obj.success) {
                    Rbac.view.tab.ActivityMonitor.init({
                        verbose: true,
                        maxInactive: obj.keepaliveDelay * 1000 * 60,
                        isInactive: function(){
                            Ext.Ajax.request({
                                url: '/tab/rbac/UILogout',
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                async: true,
                                failure: function (response, opts) {
                                    console.log('server-side failure with status code ' + response.status);
                                    window.location.href = "/login.html";
                                },
                                success: function (response, opts) {
                                    window.location.href = "/login.html";
                                }
                            });
                        },
                        isActive: function(){
                            Ext.Ajax.request({
                                url: '/tab/rbac/UIIsLogin',
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                failure: function (response, opts) {
                                    console.log('server-side failure with status code ' + response.status);
                                    window.location.href = "/login.html";
                                },
                                success: function (response, options) {
                                    var obj = Ext.decode(response.responseText);
                                    if (obj != null && obj.success) {
                                    }else{
                                        window.location.href = "/login.html";
                                    }
                                }
                            });
                        }
                    });
                    Rbac.view.tab.ActivityMonitor.start();
                } else {
                    window.location.href = "/login.html";
                }
            }
        });
    },

    onAppUpdate: function () {
        Ext.Msg.confirm('Application Update', 'This application has an update, reload?',
            function (choice) {
                if (choice === 'yes') {
                    window.location.reload();
                }
            }
        );
    }
});