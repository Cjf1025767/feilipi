
/*
两个下拉框输入时间的组件，特点是简单好理解
*/
Ext.define('Tab.view.tab.datetimetowfield', {
    extend:'Ext.form.FieldContainer',
    alias: 'widget.datetimetowfield',
	mixins : {
		field : 'Ext.form.field.Field'
	},
	layout : 'hbox',
	dateCfg : {},
	timeCfg : {},
    initComponent : function() {
    	var me = this;
    	me.buildField();
    	me.callParent();
    	this.dateField = this.down('datefield');
    	this.timeField = this.down('timefield');
    },
    // @private
    buildField : function() {
    	this.items = [Ext.apply({
	    	xtype : 'datefield',
	    	//anchor: '100%', //和colm组件配合
	    	format : 'Y-m-d',
	    	allowBlank : false,
	    	editable : true,
	    	flex : 3,
	    	value:new Date()
    	}, this.dateCfg), Ext.apply({
	    	xtype : 'timefield',
			fieldStyle:'padding:5px 6px 4px',
	    	format : 'H:i',
	    	allowBlank : false,
	    	editable : true,
	    	submitFormat : 'H:i',
	    	flex : 2
    	}, this.timeCfg)]
    },
    getValue : function() {
    	var value, date = this.dateField.getSubmitValue(), 
    	time = this.timeField.getSubmitValue();
    	if (date) {
	    	if (time) {
		    	var format = this.getFormat()
		    	value = Ext.Date.parse(date + ' ' + time, format)
	    	} else {
	    		value = this.dateField.getValue()
	    	}
    	}
    	return value
    },
    getSubmitValue : function() {
    	var value, date = this.dateField.getSubmitValue(), 
    	time = this.timeField.getSubmitValue();
    	if (date) {
	    	if (time) {
		    	return date + ' ' + time;
	    	} else {
	    		return date;
	    	}
    	}
    	return null;
    },
    setValue : function(value) {
    	var str = value.substring(0,value.indexOf(' ')); 
    	this.dateField.setValue(str)
    	str = value.substring(value.indexOf(' ') + 1); 
    	str = str.substring(0,str.lastIndexOf(':')); 
    	this.timeField.setValue(str);
    },
    getSubmitData : function() {
    	var value = this.getValue()
    	var format = this.getFormat()
    	return value ? Ext.Date.format(value, format) : null;
    },
    getFormat : function() {
    	return (this.dateField.submitFormat || this.dateField.format) + " "
    	+ (this.timeField.submitFormat || this.timeField.format)
    }
});

function PrefixInteger(num, length) {
	return (Array(length).join('0') + num).slice(-length);
}