(ns pinaclj-rich-text-editor.editor
  (:require [pinaclj-rich-text-editor.toggles :as toggles]
            [pinaclj-rich-text-editor.paragraph :as paragraph]
            [pinaclj-rich-text-editor.character :as character]))

(defn insert-into-loc [state c]
  (-> state
      (paragraph/handle c)
      (toggles/handle c)
      (character/handle c)))

(defn initialize [state]
  (toggles/initialize state))
