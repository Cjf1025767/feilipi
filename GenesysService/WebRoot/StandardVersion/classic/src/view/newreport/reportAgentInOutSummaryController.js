Ext.define('Tab.view.newreport.reportAgentInOutSummaryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.newreport-reportagentinoutsummary',
    
    init: function (view) {
        this.groupingFeature = view.view.findFeature('grouping');
    },
    onClearGroupingClick: function () {
        this.groupingFeature.disable();
    },
    onCollapseAll: function () {
        this.groupingFeature.collapseAll();
    },
    onExpandAll: function () {
        this.groupingFeature.expandAll();
    },
    onToggleGroup: function (item) {
        this.groupingFeature[item.checked ? 'expand' : 'collapse'](item.text, {
            highlight: true
        });
    },
    myOwnFn: function(records, measure, matrix, rowGroupKey, colGroupKey){
        // custom aggregate function
        return records.length;
    }

});
