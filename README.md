# PostHere.io

[PostHere.io](http://posthere.io/) is a simple service for developers to help debug API calls and web hooks. 

As a developer, if your code is POSTing to someone else’s code… it can be hard to iterate and validate your code without the burden of test mocking (tedious and not a complete test) or servicing your own POST requests (wasted code and effort).

Wouldn’t it be nice to be able to instantly see what your code POSTed at any time in as easy, friendly browser interface? Now you can. Just remember one URL: [posthere.io](http://posthere.io/). 

Decide if you want to use HTTP or HTTPs, and come up with a string that makes sense for the POST you’re attempting, and viola! 

    **https**://posthere.io/**test-my-twitter-code**

    **http**://posthere.io/**a-webhook-callback-test**

    **http**://posthere.io/**another-webhook-callback-test?api=123**

After POSTing your JSON, XML, or form fields, point your web browser at the same URL and you’ll see a clear history and details of all the POST requests you made. Neat, huh?

Need to simulate different HTTP responses to your POST, to ensure your code can handle them? It’s easy:

    **https**://posthere.io/**201**/**test-my-twitter-code**

    **http**://posthere.io/**500**/**a-webhook-callback-test**

    **http**://posthere.io/**404**/another-webhook-callback-test?api=123

Don’t trust us with your test data? It’s open source! Check the code and verify we don’t do anything nefarious, or host it yourself internally.

Need more features? Fork it!

## Usage

99.9% of users should use [PostHere.io](http://posthere.io/) by just going to the [website](http://posthere.io/).

If you want to host it internally yourself, or you want to hack on the Clojure/ClojureScript code, you'll need to install Java, and [Leiningen](http://leiningen.org/).

Then you can build the ClojureScript code:

```console
lein cljsbuild once
```

And you can start a development server:

```console
lein run 
```

Then you can visit your very own PostHere.io in your browser at [http://localhost:3000/](http://localhost:3000/).

## Testing

To run the tests:

```console
lein midje
```

## License

posthere.io is distributed under the [Mozilla Public License v2.0](http://www.mozilla.org/MPL/2.0/).

Copyright © 2014 Path, Inc.