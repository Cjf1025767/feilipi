Ext.define('Rbac.model.Base', {
    extend: 'Ext.data.Model',

    schema: {
        namespace: 'Rbac.model'
    }
});

Ext.define('Rbac.model.groupsMenuModel', {
    extend: 'Ext.data.Model',
    idProperty: 'roleguid',
    fields: [{
            type: 'string',
            name: 'roleguid'
        },
        {
            type: 'string',
            name: 'rolename'
        }
    ]
});

Ext.define('Rbac.model.groupsMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.model.groupsMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetRolesManageable',
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    },
    listeners:{
    	load:{
    		scope:this,
    		fn:function(store, records){ 
                store.insert(0,Ext.create('Rbac.model.groupsMenuModel',{roleguid:'00000000-0000-0000-0000-000000000000',rolename:'全部'}));
    		}
    	}
    }
});

Ext.define('Rbac.model.groupsStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.model.groupsMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetRolesForRole',
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'Roles',
            totalProperty: 'totalCount'
        }
    }
});

Ext.define('Rbac.model.usersMenuModel', {
    extend: 'Ext.data.Model',
    idProperty: 'userguid',
    fields: [{
            type: 'string',
            name: 'userguid'
        },
        {
            type: 'string',
            name: 'nickname'
        }
    ]
});

Ext.define('Rbac.model.usersMenuStore', {
    extend: 'Ext.data.Store',
    model: 'Rbac.model.usersMenuModel',
    autoLoad: false,
    proxy: {
        type: 'rest',
        url: '/tab/rbac/UIGetUsersForRoleManageable',
        actionMethods: {
            read: 'POST'
        },
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        reader: {
            type: 'json',
            rootProperty: 'Users',
            totalProperty: 'totalCount'
        }
    },
    listeners:{
    	load:{
    		scope:this,
    		fn:function(store, records){ 
                store.insert(0,Ext.create('Rbac.model.usersMenuModel',{userguid:'00000000-0000-0000-0000-000000000000',nickname:'全部'}));
    		}
    	}
    }
});