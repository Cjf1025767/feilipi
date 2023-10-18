

Ext.define('Tab.view.report.feilipu.agentSummaryModel',{
	extend: 'Ext.data.Model',
	 fields: [
	      ]
});
Ext.define('Tab.view.report.feilipu.agentSummaryStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.feilipu.agentSummaryModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/FLPrec/UISearchAgentSummary',
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