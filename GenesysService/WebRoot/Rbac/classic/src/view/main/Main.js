Ext.define('Rbac.view.main.Main', {
    extend: 'Ext.container.Viewport',

    requires: [
        'Ext.button.Segmented',
        'Ext.list.Tree',
        'Rbac.view.tab.LogoutButton',
        'Rbac.view.tab.PasswordWindow'
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
        render: 'onMainViewRender'
    },
    tabNavigationWidth: 180, //senchaLogo的宽度和treelist的宽度同这个宽度
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
                    width: 180, //必须等于tabNavigationWidth
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
                    text: '菜单名称'
                },
                '->',
                {
                    xtype: 'label',
                    hideLabel: true,
                    allowBlank : false,
                    bind: '{userName}',
                    listeners:{
                        beforerender:'onUserNameBeforrender'
                    }
                },
                {
                    xtype: 'button',
                    ui: 'header',
                    iconCls: 'x-fa fa-keyboard-o',
                    handler: function () {
                        Ext.create({
                            xtype: 'passwordwindow'
                        }).show();
                    },
                    text: '修改密码',
                    tooltip: '修改密码'
                },
                {
                    xtype: 'logoutbutton',
                    ui: 'header',
                    href: '',
                    hrefTarget: '_self',
                    tooltip: '登出'
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
                    width: 180, //必须等于tabNavigationWidth
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