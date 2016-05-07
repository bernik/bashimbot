(ns bashimbot.core
  (:require [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]
            [clj-time.core :as time]
            [clojure.string :as s :refer [split trim join]])
  (:use [twitter.oauth]
        [twitter.api.restful]
        [twitter.request])
  (:import [java.awt  Graphics2D
                      Color
                      Font
                      RenderingHints]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO])
  (:gen-class))

(def my-creds (make-oauth-creds "APP_CONSUMER_KEY"
                                "APP_CONSUMER_SECRET"
                                "USER_ACCESS_TOKEN"
                                "USER_ACCESS_TOKEN_SECRET"))

(def state (atom {:last-update nil
                  :last-parsed-quote-id nil
                  :work? true}))

(def LINE_LENGTH 74)

(defn create-image! [lines filename]
  (let [font-size 16
        path (str "/tmp/statuses/" filename ".jpg")
        file (io/file path)
        width (+ 20 (* 10 LINE_LENGTH))
        height (+ 20
                  (* (count lines)
                     (+ font-size 10)))
        bi (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
        graphics (.createGraphics bi)]
    (doto graphics
      (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING, RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
      (.setColor (Color. 243 243 243))
      (.fillRect 0 0 width height)
      (.setColor Color/BLACK)
      (.setFont (Font. "LiberationMono" Font/PLAIN font-size)))
    (doall
      (for [i (range (count lines))
            :let [text (nth lines i)
                  y (* (inc i) (+ font-size 10))]]
        (.drawString graphics text 10 y)))
    (ImageIO/write bi "jpg" file)
    path))

(defn node->quote [n]
  {:id (-> n
           (html/select [:div.actions :span.id])
           first
           :content
           first)
   :text  (filter string?
                  (-> n
                      (html/select [:div.text])
                      first
                      :content))})

(defn last-quote []
  (-> "http://bash.im/abyssbest"
      (slurp :encoding "Windows-1251")
      (.getBytes)
      io/input-stream
      html/html-resource
      (html/select [:div.quote])
      first
      node->quote))

(defn post-text-twit! [{text :text}]
  (statuses-update :oauth-creds my-creds
                   :params {:status (join "\n" text)}))

(defn text->lines [size text]
  (map trim
   (loop [words (split text #"\s+")
          result []
          line nil]
     (let [[w & ws] words]
       (cond
         (empty? words) (if (empty? line)
                          result
                          (conj result line))
         ;; big word
         (> (count w) size) (let [[lw rw] (map #(apply str %)
                                               (split-at (- size (count line)) w))]
                              (recur
                                (assoc words 0 rw)
                                (conj result (str line lw))
                                nil))
         ;; line + word don't fit in line
         (> (count (str line w)) size) (recur
                                         (vec ws)
                                         (conj result line)
                                         (str w " "))
         ;; else
         :else (recur
                 (vec ws)
                 result
                 (str line w " ")))))))

(defn normalize-text [text]
  (->> text
       (map #(text->lines LINE_LENGTH %))
       flatten))

(defn post-image-twit! [{text :text id :id}]
  (statuses-update-with-media :oauth-creds my-creds
                              :body [(file-body-part (create-image! (normalize-text text) id))]))

(defn tick []
  (let [quote (last-quote)
        quote-length (apply + (map count (:text quote)))]
    (when-not (= (:id quote)
                 (:last-parsed-quote-id @state))
      (try
       (if (< 140 quote-length)
         (post-image-twit! quote)
         (post-text-twit! quote))
       (catch Exception e
         (println (str (.getClass e) ": " (.getMessage e)))))
      (swap! state assoc :last-parsed-quote-id (:id quote)
                         :last-update (time/now)))))
(defn -main
  "I don't do a whole lot."
  [& args]
  (swap! state assoc :last-update (time/now))
  (let [f (future (while (:work? @state)
                    (tick)
                    (Thread/sleep (* 5 60 1000))))]))
