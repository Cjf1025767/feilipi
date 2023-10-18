Ext.define('Admin.view.main.Main', {
    extend: 'Ext.container.Viewport',

    requires: [
        'Ext.button.Segmented',
        'Ext.list.Tree'
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
    tabNavigationWidth: 161, //senchaLogo的宽度和treelist的宽度同这个宽度
    items: [
        {
            xtype: 'toolbar',
            cls: 'sencha-dash-dash-headerbar shadow',
            height: 48,
            itemId: 'headerBar',
            items: [
                {
                    xtype: 'panel',
                    reference: 'senchaLogo',
                    cls: 'sencha-logo',
                    width: 161,//必须等于tabNavigationWidth
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
                    xtype: 'segmentedbutton',
                    margin: '0 16 0 0',

                    platformConfig: {
                        ie9m: {
                            hidden: true
                        }
                    },

                    items: [{
                        iconCls: 'x-fa fa-desktop',
                        pressed: true
                    }, {
                        iconCls: 'x-fa fa-tablet',
                        handler: 'onSwitchToModern',
                        tooltip: 'Switch to modern toolkit'
                    }]
                },
                {
                    iconCls:'x-fa fa-search',
                    ui: 'header',
                    href: '#searchresults',
                    hrefTarget: '_self',
                    tooltip: 'See latest search'
                },
                {
                    iconCls:'x-fa fa-envelope',
                    ui: 'header',
                    href: '#email',
                    hrefTarget: '_self',
                    tooltip: 'Check your email'
                },
                {
                    iconCls:'x-fa fa-question',
                    ui: 'header',
                    href: '#faq',
                    hrefTarget: '_self',
                    tooltip: 'Help / FAQ\'s'
                },
                {
                    iconCls:'x-fa fa-th-large',
                    ui: 'header',
                    href: '#profile',
                    hrefTarget: '_self',
                    tooltip: 'See your profile'
                },
                {
                    xtype: 'tbtext',
                    text: 'Goff Smith',
                    cls: 'top-user-name'
                },
                {
                    xtype: 'image',
                    cls: 'header-right-profile-image',
                    height: 35,
                    width: 35,
                    alt:'current user image',
                    src: 'resources/images/user-profile/2.png'
                }
            ]
        },
        {
            xtype: 'maincontainerwrap',
            id: 'main-view-detail-wrap',
            reference: 'mainContainerWrap',
            flex: 1,
            items: [
                {
                    xtype: 'treelist',
                    reference: 'navigationTreeList',
                    itemId: 'navigationTreeList',
                    ui: 'navigation',
                    store: 'NavigationTree',
                    width: 161, //必须等于tabNavigationWidth
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
