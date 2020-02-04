(ns elk.db)

(def default-db
  {:elk.components.code/codeblocks (let [id (gensym)]
                                     {id {:id id
                                          :index 0
                                          :text ""}})})
