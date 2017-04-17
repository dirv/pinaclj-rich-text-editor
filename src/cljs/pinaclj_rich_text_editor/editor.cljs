(ns pinaclj-rich-text-editor.editor
  (:require [pinaclj-rich-text-editor.strong-text :as strong-text]
            [pinaclj-rich-text-editor.paragraph :as paragraph]
            [pinaclj-rich-text-editor.character :as character]))

(defn insert-into-loc [state c]
  (-> state
      (paragraph/handle c)
      (strong-text/handle c)
      (character/handle c)))

(defn initialize [state]
  (strong-text/initialize state))
