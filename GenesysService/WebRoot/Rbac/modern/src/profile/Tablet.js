Ext.define('Rbac.profile.Tablet', {
    extend: 'Ext.app.Profile',

    requires: [
        'Rbac.view.tablet.*'
    ],

    // Map tablet/desktop profile views to generic xtype aliases:
    //
    views: {
        email: 'Rbac.view.tablet.email.Email',
        inbox: 'Rbac.view.tablet.email.Inbox',
        compose: 'Rbac.view.tablet.email.Compose',

        searchusers: 'Rbac.view.tablet.search.Users'
    },

    isActive: function () {
        return !Ext.platformTags.phone;
    }
});
