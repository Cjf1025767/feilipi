Ext.define('Tab.Application', {
    extend: 'Ext.app.Application',
    requires: ['Tab.view.tab.ActivityMonitor'],
    name: 'Tab',

    stores: [
        'NavigationTree'
    ],

    defaultToken: globalVars.defaultToken,

    mainView: 'Tab.view.main.Main',
    launch: function () {
        Ext.state.Manager.setProvider(Ext.create('Ext.state.CookieProvider', {
            expires: new Date(new Date().getTime() + (1000 * 60 * 60 * 24 * 30 * 3))
        }));
        var url = window.location.search; //获取url中"?"符后的字串
        var queryParams = new Object();
        if (url.indexOf("?") != -1) {
            var str = url.substr(1);
            strs = str.split("&");
            for(var i = 0; i < strs.length; i ++) {
                queryParams[strs[i].split("=")[0]] = decodeURI(strs[i].split("=")[1]);
            }
        }
        if(queryParams['nolist']==="1"){
            console.log("嵌入式去掉超时机制")
        }else{
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
                        Tab.view.tab.ActivityMonitor.init({
                            verbose: true,
                            maxInactive: obj.keepaliveDelay * 1000 * 60,
                            isInactive: function () {
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
                            }
                        });
                        Tab.view.tab.ActivityMonitor.start();
                    } else {
                        window.location.href = "/login.html";
                    }
                }
            });
        }
     
    }
});