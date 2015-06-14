[![Debug all the POST requests](./resources/public/images/x-all-the-y.png)](http://posthere.io/)  
[![POSThere.io](./resources/public/images/POSThere.io.tagline.png)](http://posthere.io/)

[![MPL License](http://img.shields.io/badge/license-MPL-blue.svg?style=flat)](https://www.mozilla.org/MPL/2.0/)
[![Build Status](http://img.shields.io/travis/SnootyMonkey/posthere.io.svg?style=flat)](https://travis-ci.org/SnootyMonkey/posthere.io)
[![Roadmap on Trello](http://img.shields.io/badge/roadmap-trello-blue.svg?style=flat)](https://trello.com/b/Mzjmz7jg/posthere-io-https-github-com-snootymonkey-posthere-io)

[POSThere.io](http://posthere.io/) is a simple service for debugging API calls and web hooks. 

When your code is POSTing, either to someone's API, or to their code via a web-hook you provide from your own API, it's hard to validate your code is doing the right things as you iterate. Mocking the POST request is tedious and not a complete test, and writing code to service your own POST requests is wasted code and effort.

Wouldn’t it be nice to instantly see what your code POSTed (or PUT or PATCHed) in an easy, friendly browser interface? Now you can. Just remember one URL: [posthere.io](http://posthere.io/). 


## Usage

You use [POSThere.io](http://posthere.io/) by going to the [website](http://posthere.io/). The website provides you with a unique URL to POST, PUT or PATCH to, or you can customize it.

Simply decide if you want to use HTTP or HTTPs, and use the unique default URL provided by the website or come up with a string that makes sense for the request you’re attempting, and viola! 

  **http**://posthere.io/**test-my-twitter-code**

  **https**://posthere.io/**a-webhook-callback-test**

  **https**://posthere.io/**another-webhook-callback-test/with/complexity?api=123**

After POST/PUT/PATCHing your JSON, XML, or form fields, open the same URL in your web browser and you’ll see a clear history and details of all the POST requests you made. Neat, huh?

Need to simulate different HTTP responses to your request to ensure your code can handle them? It’s easy:

  **http**://posthere.io/**test-my-twitter-code**?status=**201**

  **https**://posthere.io/**a-webhook-callback-test**?status=**500**

  **https**://posthere.io/**another/test/with/complexity?api=123**&status=**404**

If you need programmatic access to the results of your requests, make a GET request to the same URL. Do the programmatic equivalent in your language of this cURL request:

```console
curl -X GET --header "Accept: application/json" http://posthere.io/test-my-twitter-code
```


## Limits

Since POSThere.io as a free, shared resource, there are some limits. You can certainly [host it yourself](#run-it-yourself) and remove any of these limits if they are problematic:

* [POSThere.io](http://posthere.io/) captures the results of your POST, PUT and PATCH requests, but does not capture GET, DELETE or any other HTTP requests.
* The body of the request is limited to 1MB or less, or the body value won't be shown.
* Up to the last 100 requests per unique URL are captured.
* Captured requests are kept for up to 24 hours.
* Single and multi-part file uploads are not supported.
* The body of the response to the request is a fixed response and cannot be adjusted.
* You can't request a simulated timeout or delay to the request.


## Run it Yourself

Don’t trust us with your test data? No need to, it’s open source! Check the code and verify we don’t do anything nefarious, or run it yourself [internally](internal-hosting).

### Internal Hosting

To host it yourself, you'll need to install [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html), and [Leiningen](http://leiningen.org/).

First, put your own [Google Analytics tracking code](https://support.google.com/analytics/answer/1008080?hl=en) and [doorbell.io](https://doorbell.io/) app key in the `src/config.edn` configuration file if you want usage analytics and user feedback respectively. If you don't care about these features, you can leave the configuration entries blank.

Then you can create the static HTML:

```console
lein build-pages
```

And you can build the ClojureScript code:

```console
lein cljsbuild once
```

Finally, start a server:

```console
lein run!
```

Then you can visit your very own POSThere.io in your browser at: [http://localhost:3000/](http://localhost:3000/).


## Testing

Tests are run in continuous integration of the `master` and `dev` branches on [Travis CI](https://travis-ci.org/SnootyMonkey/posthere.io):

[![Build Status](http://img.shields.io/travis/SnootyMonkey/posthere.io.svg?style=flat)](https://travis-ci.org/SnootyMonkey/posthere.io)

To run the tests locally:

```console
lein midje
```


## Development and Contributing

Need more features? [Fork it!](https://github.com/SnootyMonkey/posthere.io/fork) It’s [well-tested](https://travis-ci.org/SnootyMonkey/posthere.io) Clojure and ClojureScript.

If you'd like to contribute back your enhancements (awesome!), please submit your pull requests to the `dev` branch. We promise to look at every pull request and incorporate it, or at least provide feedback on why if we won't.

* Do your best to conform to the coding style that's here... We like it.
* Use 2 soft spaces for indentation.
* Don't leave trailing spaces after lines.
* Don't leave trailing new lines at the end of files.
* Write comments.
* Write tests.
* Don't submit über pull requests, keep your changes atomic.
* Submit pull requests to the `dev` branch.
* Have fun!

## Acknowledgements

The initial idea for POSThere.io was James Ward's [echo-webhook](http://www.jamesward.com/2014/06/11/testing-webhooks-was-a-pain-so-i-fixed-the-glitch) created in June of 2014. I used echo-webhook, but it didn't really do everything it needed to, so I built POSThere.io. Thanks for the great idea James and for taking the first step.

## License

POSThere.io is distributed under the [Mozilla Public License v2.0](http://www.mozilla.org/MPL/2.0/).

Copyright © 2014-2015 Snooty Monkey, LLC.