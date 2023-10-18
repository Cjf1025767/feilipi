

Ext.define('Tab.view.report.feilipu.agent_stateModel',{
	extend: 'Ext.data.Model',
	 fields: [
		
	      ]
});
Ext.define('Tab.view.report.feilipu.agent_stateStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.feilipu.agent_stateModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/FLPrec/UISearchAgentState',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'list',
            totalProperty : 'totalCount'
        }
    }
});