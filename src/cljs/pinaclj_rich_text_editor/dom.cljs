(ns pinaclj-rich-text-editor.dom)

(defn find-node [predicate root]
  (if (predicate root)
    root
    (if (.hasChildNodes root)
      (some (partial find-node predicate) (array-seq (.-childNodes root))))))

(defn attr [k element]
  (.getAttribute element (name k)))

(defn text-node? [node]
  (= js/Node.TEXT_NODE (.-nodeType node)))

(defn children [node]
  (array-seq (.-childNodes node)))

(defn child-index [parent child]
  (.indexOf (children parent) child))

