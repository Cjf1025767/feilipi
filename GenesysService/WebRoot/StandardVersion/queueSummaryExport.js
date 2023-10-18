var Exports = Object();
Exports.Name = function(){return ""}
Exports.Header = function(){return [
	["resource_name","resource_name"],
		["time","time"],
		["media_name","media_name"],
		["interaction_type_code","interaction_type_code"],
		["interaction_subtype_code","interaction_subtype_code"],
		["entered","entered"],
		["abandoned_short","abandoned_short"],
		["abandoned_standard","abandoned_standard"],
		["abandoned_invite","abandoned_invite"],
		["distributed_","distributed_"],
		["distributed_time","distributed_time"],
		["redirected","redirected"],
		["routed_other","routed_other"],
		["accepted","accepted"],
		["accepted_thr","accepted_thr"],
		["accepted_agent","accepted_agent"],
		["accepted_agent_time","accepted_agent_time"],
		["accepted_agent_thr","accepted_agent_thr"],
		["transfer_init_agent","transfer_init_agent"],
		["invite","invite"],
		["invite_time","invite_time"],
		["engage_time","engage_time"],
		["wrap","wrap"],
		["wrap_time","wrap_time"],
		["hold","hold"],
		["hold_time","hold_time"],
		["accepted_time","accepted_time"],
		["accepted_time_max","accepted_time_max"]
		
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		lst[i] ="\t"+ records.get(i);
	}
	lst[h.indexOf('distributed_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('distributed_time')));
	lst[h.indexOf('accepted_agent_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('accepted_agent_time')));
	lst[h.indexOf('engage_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('engage_time')));
	lst[h.indexOf('invite_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('invite_time')));
	lst[h.indexOf('wrap_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('wrap_time')));
	lst[h.indexOf('hold_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('hold_time')));
	lst[h.indexOf('accepted_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('accepted_time')));
	lst[h.indexOf('accepted_time_max')] = "\t"+ standartCalculateLength(records.get(h.indexOf('accepted_time_max')));
	return lst;
}
function standartCalculateLength(d){
	var hour = Math.floor(d/(3600));//小时
	var h = d%(3600);
	var minute=Math.floor(h/(60)) ;//分钟
	var m = h%(60); 
	var second=Math.floor(m) ;
	var hh = (hour < 10 ? "0" + hour : hour)
	var mm = (minute < 10 ? "0" + minute : minute)
	var ss = (second < 10 ? "0" + second : second)
	return hh+":"+mm+":"+ss;
}