const gulp = require('gulp');
const webpack = require('webpack-stream');

gulp.task('default', ['lib', 'styles']);

gulp.task('lib', () => {
  return gulp.src('lib/js/conquery.js')
    .pipe(webpack(require('./webpack.config.js')))
    .pipe(gulp.dest('dist/'));
});

gulp.task('styles', () => {
  return gulp.src('lib/styles/**/*.sass')
    .pipe(gulp.dest('dist/styles'));
});
