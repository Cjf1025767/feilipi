Ext.define('Tab.view.widgets.Widgets', {
    extend: 'Ext.Container',
    xtype: 'widgets',

    cls: 'dashboard',
    controller: 'widgets',
    scrollable: true,

    items: [{
        xtype: 'biotile',
        height: '20em',
        // 50% width when viewport is big enough,
        // 100% when viewport is small
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        description: 'John Doe<br>Administrator',
        image: '<shared>/images/user-profile/3.png',
        banner: {
            style: 'backgroundColor: #468fd3'
        },
        footer: {
            xtype: 'followmessage'
        }
    }, {
        xtype: 'biotile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        description: 'Lucy Moon<br>Web and Graphic Designer',
        image: '<shared>/images/user-profile/4.png',
        banner: {
            style: 'backgroundColor: #8dc153'
        },
        footer: {
            xtype: 'socialbar'
        }
    }, {
        xtype: 'biotile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        description: 'Donald Brown<br>Software Engineer',
        image: '<shared>images/user-profile/1.png',
        footer: {
            xtype: 'socialstatus',
            following: 1345,
            followers: 23456,
            likes: 52678
        }
    }, {
        xtype: 'biotile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        banner: '<shared>/images/img2.jpg',
        description: 'Goff Smith<br>Project Manager',
        image: '<shared>/images/user-profile/2.png',
        footer: {
            xtype: 'followmessage'
        }
    },
    //-------------------------------------------------------------
    {
        xtype: 'statustile',
        height: 170,
        userCls: 'big-33 small-50 dashboard-item',
        shadow: true,
        color: '#167abc',
        quantity: 840,
        description: 'Sales',
        iconCls: 'x-fa fa-shopping-cart'
    }, {
        xtype: 'statustile',
        height: 170,
        userCls: 'big-33 small-50 dashboard-item',
        shadow: true,
        color: '#9cc96b',
        quantity: 611,
        description: 'Messages',
        iconCls: 'x-fa fa-envelope'
    }, {
        xtype: 'statustile',
        height: 170,
        userCls: 'big-33 small-50 dashboard-item',
        shadow: true,
        color: '#925e8b',
        quantity: 792,
        description: 'Lines of Code',
        iconCls: 'x-fa fa-code'
    }, {
        xtype: 'statustile',
        height: 170,
        userCls: 'big-33 small-50 dashboard-item',
        shadow: true,
        color: '#ffc107',
        quantity: 637,
        description: 'Users',
        iconCls: 'x-fa fa-plus-circle'
    }, {
        xtype: 'statustile',
        height: 170,
        userCls: 'big-33 small-50 dashboard-item',
        shadow: true,
        color: 'green',
        quantity: 112,
        description: 'Servers',
        iconCls: 'x-fa fa-tasks'
    }, {
        xtype: 'statustile',
        height: 170,
        userCls: 'big-33 small-50 dashboard-item',
        shadow: true,
        color: '#e44959',
        quantity: 244,
        description: 'Files',
        iconCls: 'x-fa fa-file'
    },
    //-------------------------------------------------------------
    {
        xtype: 'statustile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        color: '#167abc',
        iconFirst: true,
        scale: 'large',
        quantity: 840,
        description: 'Sales',
        iconCls: 'x-fa fa-shopping-cart'
    }, {
        xtype: 'statustile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        color: '#9cc96b',
        iconFirst: true,
        scale: 'large',
        quantity: 611,
        description: 'Messages',
        iconCls: 'x-fa fa-envelope'
    }, {
        xtype: 'statustile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        color: '#925e8b',
        iconFirst: true,
        scale: 'large',
        quantity: 792,
        description: 'Lines of Code',
        iconCls: 'x-fa fa-code'
    }, {
        xtype: 'statustile',
        height: '20em',
        userCls: 'big-50 small-100 dashboard-item',
        shadow: true,
        color: '#e44959',
        iconFirst: true,
        scale: 'large',
        quantity: 244,
        description: 'Files',
        iconCls: 'x-fa fa-file-text'
    }]
});
