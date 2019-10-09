(ns elk.components.code)

(defn unit []
  [:div.column
   [:div.row
    (let [id (gensym)]
      [:div
       [:label {:for id} "name"]
       [:input {:type :text
                :id id}]])
    (let [id (gensym)]
      [:div
       [:label {:for id} "metadata"]
       [:input {:id id :type :text}]])]
   [:div.editor]])
