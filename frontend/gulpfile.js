const gulp = require('gulp');
const babel = require('gulp-babel');

gulp.task('default', ['js', 'styles', 'images', 'localization']);

gulp.task('js', () => {
  return gulp.src('lib/js/**/*.js')
    .pipe(babel())
    .pipe(gulp.dest('dist/js'));
});

gulp.task('styles', () => {
  return gulp.src('lib/styles/**/*.sass')
    .pipe(gulp.dest('dist/styles'));
});

gulp.task('images', () => {
  return gulp.src('lib/images/**/*')
    .pipe(gulp.dest('dist/images'));
});

gulp.task('localization', () => {
  return gulp.src('lib/localization/**/*.yml')
    .pipe(gulp.dest('dist/localization'));
});
