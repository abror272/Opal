# Third-party components

## open_nsfw (NSFW image classification model)

- **File:** `composeApp/src/androidMain/assets/nsfw.tflite` (5.7 MB)
- **Source:** [yahoo/open_nsfw](https://github.com/yahoo/open_nsfw) — TFLite conversion distributed via
  [devzwy/open_nsfw_android](https://github.com/devzwy/open_nsfw_android) /
  [davewang/open_nsfw_ios](https://github.com/davewang/open_nsfw_ios)
- **License:** BSD 2-Clause (model and code), see
  [Caffe LICENSE](https://github.com/BVLC/caffe/blob/master/LICENSE)
- **Usage:** on-device inference only. Input 224×224, BGR, mean `[104, 117, 123]`;
  output `[sfw, nsfw]`. Screenshots are processed in memory and never stored or uploaded.

## TensorFlow Lite

- **Dependency:** `org.tensorflow:tensorflow-lite` (Apache License 2.0)
- **Usage:** local interpreter for the model above. No network calls.
