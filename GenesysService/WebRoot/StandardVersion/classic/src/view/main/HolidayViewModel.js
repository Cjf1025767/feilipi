Ext.define('Tab.view.main.HolidayViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main-holidayview'
});

Ext.define('Tab.view.main.holidayModel',{
    extend: 'Ext.data.Model',
    idProperty:'callholidayid',
    fields: [
        {name:'activate',type:'bool',mapping:function(v){
            return ((v.activate&1)==1);
            }
        }
    ]
});

Ext.define('Tab.view.main.holidayStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.holidayModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListHolidays',
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

Ext.define('Tab.view.main.worktimeStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.holidayModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListHolidays',
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

Ext.define('Tab.view.main.phoneModel',{
    extend: 'Ext.data.Model',
    idProperty:'callholidayid',
    fields: [
    ]
});

Ext.define('Tab.view.main.workphoneStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.main.phoneModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/ListHolidaysWeek',
		timeout: 120000,
        actionMethods : {read: 'POST'},
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'phones',
            totalProperty: 'total'
        }
    }
});