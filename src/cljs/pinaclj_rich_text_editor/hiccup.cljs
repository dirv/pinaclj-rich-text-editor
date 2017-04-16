(ns pinaclj-rich-text-editor.hiccup
  )

(defn- attr [[tag attrs-or-child] k]
  (when (map? attrs-or-child)
    (get attrs-or-child k)))

(defn matches-attr? [k v node]
  (= v (attr node k)))

