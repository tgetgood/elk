(ns elk.dispatch)

(re-frame/reg-event-fx
 ::dispatch
 (fn [cofx [_ trans msg]]
   ;; So here both trans and msg are messages. We need to vivify trans and then
   ;; apply it to msg
   ))
