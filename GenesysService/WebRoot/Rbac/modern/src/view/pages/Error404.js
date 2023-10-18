Ext.define('Rbac.view.pages.Error404', {
    extend: 'Rbac.view.pages.ErrorBase',
    xtype:'page404',

    items:[{
        cls: 'error-page-top-text',
        html: '404'
    },{
        cls: 'error-page-desc',
        html: '<p>Seems you\'ve hit a wall!</p><p>Try going back to our ' +
        '<a href="#dashboard"> Home page </a></p>'
    }]
});
