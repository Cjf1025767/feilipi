Ext.define('Tab.view.main.Main', {
    extend: 'Ext.container.Viewport',

    requires: [
        'Ext.button.Segmented',
        'Ext.list.Tree',
        'Tab.view.tab.LogoutButton',
        'Tab.view.tab.PasswordWindow'
    ],

    controller: 'main',
    viewModel: 'main',

    cls: 'sencha-dash-viewport',
    itemId: 'mainView',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    listeners: {
        beforerender: 'onNavigationTreeBeforeRender',
        render: 'onMainViewRender',
        afterrender:function(){
            var me = this;
            //使用ExtJS的cookie保持当前状态
            Ext.state.Manager.setProvider(new Ext.state.Provider({expires: new Date(new Date().getTime()+(1000*60*60*24*30*3))}));//cookie超时时间：90 days
            //加载根目录js下的中文语言包,不用拷贝ext库目录
            //Ext.Loader.loadScript({
            //    url:['/js/foo.js']
            //});
        }
    },
    tabNavigationWidth: 211, //senchaLogo的宽度和treelist的宽度同这个宽度
    items: [{
            xtype: 'toolbar',
            cls: 'sencha-dash-dash-headerbar shadow',
            height: 48,
            reference: 'headerBar',
            itemId: 'headerBar',
            items: [{
                    xtype: 'panel',
                    reference: 'senchaLogo',
                    cls: 'sencha-logo',
                    width: 211, //必须等于tabNavigationWidth
                    layout: 'hbox',
                    items: [{
                            xtype: 'button',
                            border: false,
                            iconCls: 'x-fa fa-backward',
                            style: "width:48px;height:48px",
                            handler: 'onToggleNavigationSize'
                        },
                        {
                            xtype: 'component',
                            cls: 'sencha-logo',
                            style: "width:186px;height:48px",
                            html: '<div class="main-logo">TAB</div>',
                            margin: '0 0 0 0'
                        }
                    ]
                },
                {
                    margin: '0 0 0 8',
                    itemId: 'menuBar',
                    ui: 'header',
                    iconCls: 'x-fa fa-bars',
                    text: 'Menu'
                },
                '->',
//                {
//                    xtype: 'button',
//                    ui: 'header',
//                    iconCls: 'x-fa fa-keyboard-o',
//                    handler: function () {
//                        Ext.create({
//                            xtype: 'smsview'
//                        }).show();
//                    },
//                    text: 'Send SMS',
//                    tooltip: 'Verify SMS'
//                },
				{
                    xtype: 'button',
                    itemId:'softphoneId',
                    hidden:true,
                    agent:"",
                    phoneWindow:null,
                    iconCls: 'x-fa fa-headphones',
                    ui: 'header',
                    text: 'Headphones',
                    tooltip: 'Headphones',
					handler: function (btn) {
                        var me = btn;
                        Ext.Ajax.request({
                            url: '/tab/call/GetAgentInfo',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    if(obj.agent.length>0){
                                        me.agent = obj.agent;
                                        me.extension = obj.extension;
                                        if (me.phoneWindow === null || me.phoneWindow.closed) {
                                            var url = window.location.protocol + "//" + window.location.hostname + (window.location.port.length>0?(":"+window.location.port):"");
                                            me.phoneWindow = window.open(url + "/tab/agent/SetAgentInfo/?agent=" + me.agent + (me.extension ? "&extension=" + me.extension : ""), "tabcrm");
                                        } else {
                                            me.phoneWindow.focus();
                                        }
                                    }else{
                                        Ext.Msg.alert("Attention","Please configure User's VoIP softphone correctly["+obj.username+"]");
                                    }
                                }else{
                                    Ext.Msg.alert("Attention","Please configure VoIP softphone correctly");
                                }
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    },
                },
                {
                    iconCls: 'x-fa fa-users',
                    hidden:true,
                    itemId:'usermanagerId',
                    ui: 'header',
                    href: '/Rbac/index.html#norecord',
                    hrefTarget: '_blank',
                    text: 'User management',
                    tooltip: 'User management'
                },
                // {
                //     xtype: 'button',
                //     ui: 'header',
                //     iconCls: 'x-fa fa-keyboard-o',
                //     handler: function () {
                //         Ext.create({
                //             xtype: 'passwordwindow'
                //         }).show();
                //     },
                //     text: 'Change Password',
                //     tooltip: 'Change Password'
                // },
                {
                    xtype: 'logoutbutton',
                    ui: 'header',
                    href: '',
                    hrefTarget: '_self',
                    tooltip: 'Logout'
                }
            ]
        },
        {
            xtype: 'maincontainerwrap',
            id: 'main-view-detail-wrap',
            reference: 'mainContainerWrap',
            flex: 1,
            items: [{
                    xtype: 'treelist',
                    reference: 'navigationTreeList',
                    itemId: 'navigationTreeList',
                    ui: 'navigation',
                    store: 'NavigationTree',
                    width: 211, //必须等于tabNavigationWidth
                    expanderFirst: false,
                    expanderOnly: false,
                    listeners: {
                        selectionchange: 'onNavigationTreeSelectionChange'
                    }
                },
                {
                    xtype: 'container',
                    flex: 1,
                    reference: 'mainCardPanel',
                    cls: 'sencha-dash-right-main-container',
                    itemId: 'contentPanel',
                    layout: {
                        type: 'card',
                        anchor: '100%'
                    }
                }
            ]
        }
    ]
});