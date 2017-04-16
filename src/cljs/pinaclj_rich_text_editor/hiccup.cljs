(ns pinaclj-rich-text-editor.hiccup)

(defn- attr [[tag attrs-or-child] k]
  (when (map? attrs-or-child)
    (get attrs-or-child k)))

(defn matches-attr? [k v node]
  (= v (attr node k)))

(defn insert-attr [[tag & [attrs-or-first-child & remaining-children :as all-children] :as node] k v]
  (if (nil? (attr node k))
    (if (map? attrs-or-first-child)
      (apply vector tag (assoc attrs-or-first-child k v) remaining-children)
      (apply vector tag {k v} all-children))
    node))

