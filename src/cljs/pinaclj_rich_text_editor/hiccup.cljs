(ns pinaclj-rich-text-editor.hiccup)

(defn- attr [[tag attrs-or-child] k]
  (when (map? attrs-or-child)
    (get attrs-or-child k)))

(defn tag [node]
  (first node))

(defn matches-attr? [k v node]
  (= v (attr node k)))

(defn matches-tag? [tag node]
  (and (vector? node) (= tag (first node))))

(defn insert-attr [[tag & [attrs-or-first-child & remaining-children :as all-children] :as node] k v]
  (if (nil? (attr node k))
    (if (map? attrs-or-first-child)
      (apply vector tag (assoc attrs-or-first-child k v) (or remaining-children []))
      (apply vector tag {k v} (or all-children [])))
    node))

(defn set-attr [[tag & [attrs-or-first-child & remaining-children :as all-children] :as node] k v]
  (if (map? attrs-or-first-child)
    (apply vector tag (assoc attrs-or-first-child k v) (or remaining-children []))
    (apply vector tag {k v} (or all-children []))))

(defn children [[_ & [attrs-or-first-child & remaining-children :as all-children]]]
  (if (map? attrs-or-first-child) remaining-children all-children))

(defn replace-children [[tag & existing] replace-children]
  (if (map? (first existing))
    (into [tag (first existing)] replace-children)
    (into [tag] replace-children)))

(defn map-hiccup [f node]
  (if (vector? node)
    (replace-children (f node) (mapv (partial map-hiccup f) (children node)))
    (f node)))
