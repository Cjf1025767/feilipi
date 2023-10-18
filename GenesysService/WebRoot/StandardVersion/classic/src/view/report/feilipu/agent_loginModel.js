
Ext.define('Tab.view.report.feilipu.agent_loginModel',{
	extend: 'Ext.data.Model',
	 fields: [
	      ]
});
Ext.define('Tab.view.report.feilipu.agent_loginStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.feilipu.agent_loginModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
        url: '/tab/FLPrec/UISearchAgentLogin',
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