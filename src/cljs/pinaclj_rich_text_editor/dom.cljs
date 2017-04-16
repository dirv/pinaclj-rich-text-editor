(ns pinaclj-rich-text-editor.dom)

(defn find-node [predicate root]
  (if (predicate root)
    root
    (if (.hasChildNodes root)
      (some (partial find-node predicate) (array-seq (.-childNodes root))))))
