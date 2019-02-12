const { series, src, dest } = require('gulp');
const babel = require('gulp-babel');

function js() {
  return src('lib/js/**/*.js')
    .pipe(babel())
    .pipe(dest('dist/js'));
};

function styles() {
  return src('lib/styles/**/*.sass')
    .pipe(dest('dist/styles'));
};

function images() {
  return src('lib/images/**/*')
    .pipe(dest('dist/images'));
};

function localization() {
  return src('lib/localization/**/*.yml')
    .pipe(dest('dist/localization'));
};

exports.default = series(js, styles, images, localization);
