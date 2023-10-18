var Exports = Object();
Exports.Name = function(){return ""}
Exports.Header = function(){return [
	["start_date_time_string","start time"],
	["end_date_time_string","end time"],
	["ring_time","ring_time"],
	["answer_time","answer_time"],
	["dialing_time","dialing_time"],
	["agent_last_name","agent_last_name"],
    ["interaction_type","interaction_type"],
    ["interaction_id","interaction_id"],
    ["media_server_ixn_guid","media_server_ixn_guid"],
    ["party_name","party_name"],
    ["source_address","source_address"],
    ["target_address","target_address"],
    ["mediation_duration","mediation_duration"],
    ["routing_point_duration","routing_point_duration"],
    ["talk_duration","talk_duration"],
    ["ring_duration","ring_duration"],
    ["dial_duration","dial_duration"],
    ["hold_duration","hold_duration"],
    ["after_call_work_duration","after_call_work_duration"],
    ["stop_action","stop_action"],
    ["resource_role","resource_role"],
    ["role_reason","role_reason"],
    ["technical_result","technical_result"],
    ["result_reason","result_reason"],
    ["queues","queues"],
    ["media_name","media_name"],
    ["transfer_agent_name","transfer_agent_name"],
    ["transfer_employee_id","transfer_employee_id"]

];}
Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
        lst[i] ="\t"+ records.get(i);
	}
	lst[h.indexOf('mediation_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('mediation_duration')));
    lst[h.indexOf('routing_point_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('routing_point_duration')));
    lst[h.indexOf('talk_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('talk_duration')));
    lst[h.indexOf('ring_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('ring_duration')));
    lst[h.indexOf('dial_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('dial_duration')));
    lst[h.indexOf('hold_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('hold_duration')));
    lst[h.indexOf('after_call_work_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('after_call_work_duration')));
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


