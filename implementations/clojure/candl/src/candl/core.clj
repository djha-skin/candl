(ns candl
  (:require [instaparse.core :as insta]))

(def -ast-generator
  (insta/parser (clojure.java.io/resource "candl/candl.abnf")
    :input-format :abnf))

(ast-generator (clojure.java.io/resource "candl/example.cdl"))

(defn parse
  [{:keys [string constructors]}]
