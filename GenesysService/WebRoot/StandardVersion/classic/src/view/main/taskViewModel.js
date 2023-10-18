Ext.define('Tab.view.main.taskViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-taskview',
    data: {
        name: 'Tab'
    }

});


Ext.define('Tab.view.main.taskModel',{
    extend: 'Ext.data.Model',
    idProperty:'id',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.status&1)==1);
        }},
        {name:'complete',type:'bool',mapping:function(v){
            return ((v.status&2)==2);
        }},
        {name:'running',type:'bool',mapping:function(v){
            return ((v.status&4)==4);
        }},
        {name:'queue',type:'bool',mapping:function(v){
            return ((v.status&8)==8);
        }}
    ]
});

Ext.define('Tab.view.main.taskStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.taskModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListTask',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'items',
            totalProperty: 'total'
        }
    }
});