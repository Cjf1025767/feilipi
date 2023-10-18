Ext.define('Tab.view.main.agentgroupViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-agentgroupview',
    data: {
        name: 'Tab'
    }

});

Ext.define('Tab.view.main.agentgroupModel',{
    extend: 'Ext.data.Model',
    idProperty:'_id',
    fields: [
        {name:'role.rolename',mapping:'role.rolename'}
    ]
});

Ext.define('Tab.view.main.agentgroupStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.agentgroupModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListAgentQueue',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'users',
            totalProperty: 'total'
        }
    }
});

Ext.define('Tab.view.main.agentgroupQueuesModel',{
    extend: 'Ext.data.Model',
    idProperty:'_id',
    fields: [
    ]
});

Ext.define('Tab.view.main.agentgroupQueuesStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.agentgroupQueuesModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListQueues',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'queues'
        }
    }
});

Ext.define('Tab.view.main.agentgroupRolesModel',{
    extend: 'Ext.data.Model',
    idProperty:'_id',
    fields: [
    ]
});

Ext.define('Tab.view.main.agentgroupRolesStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.agentgroupRolesModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListRoles',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'roles'
        }
    }
});